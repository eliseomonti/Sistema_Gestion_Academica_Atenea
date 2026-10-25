package com.utn.atenea.personas.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.personas.dto.AltaEstudianteRequest;
import com.utn.atenea.personas.dto.EstudianteListadoResponse;
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

import static com.utn.atenea.personas.repository.PersonaSpecifications.estudianteCoincideCon;
import static com.utn.atenea.personas.repository.PersonaSpecifications.conInscripcion;
import static com.utn.atenea.personas.repository.PersonaSpecifications.esEstudiante;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstudianteService {

    private final PersonaService personaService;
    private final PersonaRepository personaRepository;
    private final InscripcionCarreraRepository inscripcionCarreraRepository;
    private final CarrerasApi carrerasApi;
    private final PersonaMapper personaMapper;
    private final InscripcionCarreraMapper inscripcionCarreraMapper;

    /**
     * Estudiantes con al menos una inscripción en ese estado, de la carrera indicada o de cualquiera,
     * que coinciden con el texto buscado. El orden es siempre por apellido y nombre: el que venga
     * en el request se ignora.
     */
    public Page<EstudianteListadoResponse> listar(String buscar, Long carreraId, EstadoInscripcionCarrera estado,
                                              Pageable pageable) {
        List<Specification<Persona>> filtros = new ArrayList<>(List.of(esEstudiante()));
        if (buscar != null && !buscar.isBlank()) {
            filtros.add(estudianteCoincideCon(buscar));
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
                .map(personaMapper::toEstudianteListado);
    }

    /** Alta de una persona nueva como estudiante: persona, legajo, usuario e inscripción en una sola transacción. */
    @Transactional
    public AltaPersonaResponse alta(AltaEstudianteRequest request) {
        // El plan se valida primero: si no sirve, no tiene sentido crear la persona.
        PlanEstudioResumen plan = buscarPlanQueAdmiteInscripciones(request.planEstudioId());

        Persona estudiante = personaService.crear(request.datosPersonales());
        personaService.asignarLegajoEstudiante(estudiante);
        CredencialesIniciales credenciales = personaService.crearUsuario(estudiante, NombreRol.ROLE_ESTUDIANTE);
        inscripcionCarreraRepository.save(new InscripcionCarrera(estudiante, plan.id(), LocalDate.now()));

        return new AltaPersonaResponse(estudiante.getId(), estudiante.getLegajoEstudiante(),
                credenciales.username(), credenciales.passwordTemporal());
    }

    /**
     * Inscribe en un plan a una persona que ya existe: un estudiante que suma otra carrera, o alguien
     * (por ejemplo, un docente) que todavía no es estudiante y con esto pasa a serlo.
     */
    @Transactional
    public InscripcionCarreraResponse inscribir(Long personaId, InscripcionCarreraRequest request) {
        Persona persona = personaService.buscarActiva(personaId);
        PlanEstudioResumen plan = buscarPlanQueAdmiteInscripciones(request.planEstudioId());
        validarQuePuedeInscribirse(persona, plan);

        if (!persona.esEstudiante()) {
            personaService.asignarLegajoEstudiante(persona);
            personaService.agregarRol(persona, NombreRol.ROLE_ESTUDIANTE);
        }
        InscripcionCarrera inscripcion = inscripcionCarreraRepository.save(
                new InscripcionCarrera(persona, plan.id(), LocalDate.now()));
        return inscripcionCarreraMapper.toResponse(inscripcion);
    }

    /** El estudiante deja una carrera. Conserva el legajo, el usuario y sus otras inscripciones. */
    @Transactional
    public InscripcionCarreraResponse darDeBajaInscripcion(Long personaId, Long inscripcionId) {
        InscripcionCarrera inscripcion = inscripcionCarreraRepository.findByIdAndEstudianteId(inscripcionId, personaId)
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
        if (inscripcionCarreraRepository.existsByEstudianteIdAndPlanEstudioId(persona.getId(), plan.id())) {
            throw new RecursoDuplicadoException("La persona " + persona.getId()
                    + " ya está o estuvo inscripta al plan de estudio " + plan.id());
        }
        if (inscripcionCarreraRepository.existsByEstudianteIdAndPlanEstudioIdInAndEstado(
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
