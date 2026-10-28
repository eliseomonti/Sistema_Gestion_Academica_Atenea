package com.utn.atenea.comisiones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.comisiones.dto.AltaComisionMateriaRequest;
import com.utn.atenea.comisiones.dto.ComisionMateriaResponse;
import com.utn.atenea.comisiones.dto.DocenteAsignadoRequest;
import com.utn.atenea.comisiones.dto.HorarioRequest;
import com.utn.atenea.comisiones.dto.ModificacionComisionMateriaRequest;
import com.utn.atenea.comisiones.mapper.ComisionMateriaMapper;
import com.utn.atenea.comisiones.model.Aula;
import com.utn.atenea.comisiones.model.Comision;
import com.utn.atenea.comisiones.model.ComisionMateria;
import com.utn.atenea.comisiones.RolDocente;
import com.utn.atenea.comisiones.repository.ComisionMateriaRepository;
import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las materias de una comisión: agregar, modificar (cupo, docentes y horarios) y quitar. Todo se
 * valida antes de tocar la entidad, así ninguna consulta dispara un flush a medio cambio.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComisionMateriaService {

    private final ComisionService comisionService;
    private final ComisionMateriaRepository comisionMateriaRepository;
    private final HorarioService horarioService;
    private final CarrerasApi carrerasApi;
    private final PersonasApi personasApi;
    private final ComisionMateriaMapper comisionMateriaMapper;

    @Transactional
    public ComisionMateriaResponse agregar(Long comisionId, AltaComisionMateriaRequest request) {
        Comision comision = comisionService.buscarActiva(comisionId);
        MateriaDelPlan materia = validarMateria(comision, request.materiaId());
        Map<Long, DocenteResumen> docentes = validarDocentes(request.docentes());
        Map<Long, Aula> aulas = horarioService.validar(comision, null, materia.periodoLectivo(),
                request.horarios(), docentes);

        ComisionMateria comisionMateria = comision.agregarMateria(materia.materiaId(), materia.periodoLectivo(),
                request.cupoMaximo());
        asignar(comisionMateria, request.docentes(), request.horarios(), aulas);
        // Con IDENTITY el id sale del INSERT: sin el flush, la respuesta no llevaría los ids.
        comisionMateriaRepository.flush();
        return comisionMateriaMapper.toResponse(comisionMateria,
                Map.of(materia.materiaId(), materia.nombre()), docentes);
    }

    /** Reemplaza cupo, docentes y horarios: lo que no venga en el request se quita. */
    @Transactional
    public ComisionMateriaResponse modificar(Long comisionId, Long comisionMateriaId,
                                             ModificacionComisionMateriaRequest request) {
        Comision comision = comisionService.buscarActiva(comisionId);
        ComisionMateria comisionMateria = buscar(comisionId, comisionMateriaId);
        Map<Long, DocenteResumen> docentes = validarDocentes(request.docentes());
        Map<Long, Aula> aulas = horarioService.validar(comision, comisionMateria,
                comisionMateria.getPeriodoLectivo(), request.horarios(), docentes);

        comisionMateria.setCupoMaximo(request.cupoMaximo());
        comisionMateria.quitarDocentesYHorarios();
        // Hibernate hace los INSERT antes que los DELETE: un horario que se reenvía igual chocaría
        // con su versión vieja en el UNIQUE (comision_materia_id, dia_semana, hora_inicio).
        comisionMateriaRepository.flush();
        asignar(comisionMateria, request.docentes(), request.horarios(), aulas);
        comisionMateriaRepository.flush();

        String nombre = carrerasApi.buscarMateriaDelPlan(comision.getPlanEstudioId(), comisionMateria.getMateriaId())
                .map(MateriaDelPlan::nombre)
                .orElse(null);
        return comisionMateriaMapper.toResponse(comisionMateria,
                Collections.singletonMap(comisionMateria.getMateriaId(), nombre), docentes);
    }

    /**
     * Quita la materia con sus docentes y horarios: es para corregir una carga errónea. Si ya tiene
     * inscripciones, evaluaciones o encuestas, lo impiden las FK RESTRICT de la base.
     */
    @Transactional
    public void quitar(Long comisionId, Long comisionMateriaId) {
        Comision comision = comisionService.buscarActiva(comisionId);
        comision.quitarMateria(buscar(comisionId, comisionMateriaId));
        try {
            comisionMateriaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("La materia " + comisionMateriaId + " de la comisión " + comisionId
                    + " tiene inscripciones, evaluaciones o encuestas: no se puede quitar");
        }
    }

    private ComisionMateria buscar(Long comisionId, Long comisionMateriaId) {
        return comisionMateriaRepository.findByIdAndComisionId(comisionMateriaId, comisionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La comisión " + comisionId + " no tiene la materia " + comisionMateriaId));
    }

    // La materia tiene que ser del plan y del año de la carrera de la comisión, y no estar ya en ella.
    private MateriaDelPlan validarMateria(Comision comision, Long materiaId) {
        MateriaDelPlan materia = carrerasApi.buscarMateriaDelPlan(comision.getPlanEstudioId(), materiaId)
                .orElseThrow(() -> new ReglaNegocioException("La materia " + materiaId
                        + " no es del plan de estudio " + comision.getPlanEstudioId()));
        if (materia.anioCursado() != comision.getAnioCursado()) {
            throw new ReglaNegocioException(materia.nombre() + " es de " + materia.anioCursado()
                    + "° año y la comisión es de " + comision.getAnioCursado() + "° año");
        }
        if (comision.getMaterias().stream().anyMatch(m -> m.getMateriaId().equals(materiaId))) {
            throw new RecursoDuplicadoException(materia.nombre() + " ya está en la comisión " + comision.getNombre());
        }
        return materia;
    }

    /**
     * Sin repetidos, como máximo un titular, y todos docentes activos. Devuelve los docentes por id:
     * con ellos se arman la respuesta y los mensajes de los choques de horario.
     */
    private Map<Long, DocenteResumen> validarDocentes(List<DocenteAsignadoRequest> asignados) {
        Set<Long> ids = new LinkedHashSet<>();
        for (DocenteAsignadoRequest asignado : asignados) {
            if (!ids.add(asignado.docenteId())) {
                throw new ReglaNegocioException("El docente " + asignado.docenteId() + " está más de una vez");
            }
        }
        if (asignados.stream().filter(asignado -> asignado.rol() == RolDocente.TITULAR).count() > 1) {
            throw new ReglaNegocioException("La materia puede tener como máximo un titular");
        }

        Map<Long, DocenteResumen> docentes = personasApi.buscarDocentes(ids).stream()
                .collect(Collectors.toMap(DocenteResumen::personaId, Function.identity()));
        for (Long id : ids) {
            DocenteResumen docente = docentes.get(id);
            if (docente == null) {
                throw new RecursoNoEncontradoException("No existe el docente con id " + id);
            }
            if (!docente.activo()) {
                throw new ReglaNegocioException("El docente " + docente.apellido() + ", " + docente.nombre()
                        + " está dado de baja");
            }
        }
        return docentes;
    }

    // Un horario sin aula (cursada virtual) queda con aula null.
    private static void asignar(ComisionMateria comisionMateria, List<DocenteAsignadoRequest> docentes,
                                List<HorarioRequest> horarios, Map<Long, Aula> aulas) {
        docentes.forEach(docente -> comisionMateria.asignarDocente(docente.docenteId(), docente.rol()));
        horarios.forEach(horario -> comisionMateria.agregarHorario(horario.diaSemana(), horario.horaInicio(),
                horario.horaFin(), horario.aulaId() != null ? aulas.get(horario.aulaId()) : null));
    }
}
