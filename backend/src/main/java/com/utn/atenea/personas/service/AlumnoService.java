package com.utn.atenea.personas.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.personas.dto.AltaAlumnoRequest;
import com.utn.atenea.personas.dto.AlumnoListadoResponse;
import com.utn.atenea.personas.dto.AltaPersonaResponse;
import com.utn.atenea.personas.dto.InscripcionCarreraRequest;
import com.utn.atenea.personas.dto.InscripcionCarreraResponse;
import com.utn.atenea.personas.mapper.InscripcionCarreraMapper;
import com.utn.atenea.personas.mapper.PersonaMapper;
import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.model.InscripcionCarrera;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.InscripcionCarreraRepository;
import com.utn.atenea.personas.repository.PersonaRepository;
import com.utn.atenea.seguridad.CredencialesIniciales;
import com.utn.atenea.seguridad.NombreRol;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.utn.atenea.personas.repository.PersonaSpecifications.alumnoCoincideCon;
import static com.utn.atenea.personas.repository.PersonaSpecifications.conInscripcion;
import static com.utn.atenea.personas.repository.PersonaSpecifications.esAlumno;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlumnoService {

    private final PersonaService personaService;
    private final PersonaRepository personaRepository;
    private final InscripcionCarreraRepository inscripcionCarreraRepository;
    private final CarrerasApi carrerasApi;
    private final PersonaMapper personaMapper;
    private final InscripcionCarreraMapper inscripcionCarreraMapper;

    /**
     * Alumnos con al menos una inscripción en ese estado, de la carrera indicada o de cualquiera,
     * que coinciden con el texto buscado. El orden es siempre por apellido y nombre: el que venga
     * en el request se ignora.
     */
    public Page<AlumnoListadoResponse> listar(String buscar, Long carreraId, EstadoInscripcionCarrera estado,
                                              Pageable pageable) {
        List<Specification<Persona>> filtros = new ArrayList<>(List.of(esAlumno()));
        if (buscar != null && !buscar.isBlank()) {
            filtros.add(alumnoCoincideCon(buscar));
        }

        List<Long> planesDeLaCarrera = null;
        if (carreraId != null) {
            planesDeLaCarrera = carrerasApi.idsDePlanes(carreraId);
            if (planesDeLaCarrera.isEmpty()) {
                return Page.empty(pageable);
            }
        }
        filtros.add(conInscripcion(estado, planesDeLaCarrera));

        return personaRepository.findAll(Specification.allOf(filtros), PersonaService.ordenAlfabetico(pageable))
                .map(personaMapper::toAlumnoListado);
    }

    /** Alta de una persona nueva como alumno: persona, legajo, usuario e inscripción en una sola transacción. */
    @Transactional
    public AltaPersonaResponse alta(AltaAlumnoRequest request) {
        // El plan se valida primero: si no sirve, no tiene sentido crear la persona.
        PlanEstudioResumen plan = buscarPlanQueAdmiteInscripciones(request.planEstudioId());

        Persona alumno = personaService.crear(request.datosPersonales());
        personaService.asignarLegajoAlumno(alumno);
        CredencialesIniciales credenciales = personaService.crearUsuario(alumno, NombreRol.ROLE_ALUMNO);
        inscripcionCarreraRepository.save(new InscripcionCarrera(alumno, plan.id(), LocalDate.now()));

        return new AltaPersonaResponse(alumno.getId(), alumno.getLegajoAlumno(),
                credenciales.username(), credenciales.passwordTemporal());
    }

    /**
     * Inscribe en un plan a una persona que ya existe: un alumno que suma otra carrera, o alguien
     * (por ejemplo, un docente) que todavía no es alumno y con esto pasa a serlo.
     */
    @Transactional
    public InscripcionCarreraResponse inscribir(Long personaId, InscripcionCarreraRequest request) {
        Persona persona = personaService.buscarActiva(personaId);
        PlanEstudioResumen plan = buscarPlanQueAdmiteInscripciones(request.planEstudioId());
        validarQuePuedeInscribirse(persona, plan);

        if (!persona.esAlumno()) {
            personaService.asignarLegajoAlumno(persona);
            personaService.agregarRol(persona, NombreRol.ROLE_ALUMNO);
        }
        InscripcionCarrera inscripcion = inscripcionCarreraRepository.save(
                new InscripcionCarrera(persona, plan.id(), LocalDate.now()));
        return inscripcionCarreraMapper.toResponse(inscripcion);
    }

    /** El alumno deja una carrera. Conserva el legajo, el usuario y sus otras inscripciones. */
    @Transactional
    public InscripcionCarreraResponse darDeBajaInscripcion(Long personaId, Long inscripcionId) {
        InscripcionCarrera inscripcion = inscripcionCarreraRepository.findByIdAndAlumnoId(inscripcionId, personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La persona " + personaId + " no tiene la inscripción " + inscripcionId));
        if (inscripcion.getEstado() != EstadoInscripcionCarrera.ACTIVA) {
            throw new ReglaNegocioException("La inscripción " + inscripcionId
                    + " no está activa: su estado es " + inscripcion.getEstado());
        }

        inscripcion.darDeBaja(LocalDate.now());
        return inscripcionCarreraMapper.toResponse(inscripcion);
    }

    // inscripcion_carrera guarda el plan, no la carrera: para saber si ya cursa esa carrera se
    // buscan sus inscripciones activas entre todos los planes de la carrera.
    private void validarQuePuedeInscribirse(Persona persona, PlanEstudioResumen plan) {
        if (inscripcionCarreraRepository.existsByAlumnoIdAndPlanEstudioId(persona.getId(), plan.id())) {
            throw new RecursoDuplicadoException("La persona " + persona.getId()
                    + " ya está o estuvo inscripta al plan de estudio " + plan.id());
        }
        if (inscripcionCarreraRepository.existsByAlumnoIdAndPlanEstudioIdInAndEstado(
                persona.getId(), carrerasApi.idsDePlanes(plan.carreraId()), EstadoInscripcionCarrera.ACTIVA)) {
            throw new ReglaNegocioException("La persona " + persona.getId()
                    + " ya tiene una inscripción activa en otro plan de esa carrera");
        }
    }

    private PlanEstudioResumen buscarPlanQueAdmiteInscripciones(Long planEstudioId) {
        PlanEstudioResumen plan = carrerasApi.buscarPlan(planEstudioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el plan de estudio con id " + planEstudioId));
        if (!plan.admiteInscripciones()) {
            throw new ReglaNegocioException("El plan de estudio " + planEstudioId
                    + " no admite inscripciones: está discontinuado o su carrera está dada de baja");
        }
        return plan;
    }
}
