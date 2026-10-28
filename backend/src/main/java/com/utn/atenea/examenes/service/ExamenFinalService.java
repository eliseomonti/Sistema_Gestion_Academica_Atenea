package com.utn.atenea.examenes.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.comisiones.AulaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.PeriodoDeFinal;
import com.utn.atenea.examenes.PeriodosDeFinal;
import com.utn.atenea.examenes.dto.AltaExamenFinalRequest;
import com.utn.atenea.examenes.dto.ExamenFinalFiltroRequest;
import com.utn.atenea.examenes.dto.ExamenFinalResponse;
import com.utn.atenea.examenes.dto.ModificacionExamenFinalRequest;
import com.utn.atenea.examenes.mapper.ExamenFinalMapper;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.examenes.repository.ExamenFinalRepository;
import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** ABM de las mesas de examen final de un turno, para el admin. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamenFinalService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ExamenFinalRepository examenFinalRepository;
    private final PeriodosDeFinal periodosDeFinal;
    private final CarrerasApi carrerasApi;
    private final ComisionesApi comisionesApi;
    private final PersonasApi personasApi;
    private final ExamenFinalMapper examenFinalMapper;

    /** Las mesas del turno, por fecha y hora. */
    public List<ExamenFinalResponse> listar(ExamenFinalFiltroRequest filtro) {
        List<ExamenFinal> mesas = examenFinalRepository.findByPeriodoIdOrderByFechaAscHoraInicioAsc(filtro.periodoId());
        if (mesas.isEmpty()) {
            return List.of();
        }
        return toResponses(mesas, buscarTurno(filtro.periodoId()).carreraId());
    }

    public ExamenFinalResponse detalle(Long examenFinalId) {
        ExamenFinal mesa = buscar(examenFinalId);
        return toResponses(List.of(mesa), buscarTurno(mesa.getPeriodoId()).carreraId()).getFirst();
    }

    @Transactional
    public ExamenFinalResponse alta(AltaExamenFinalRequest request) {
        PeriodoDeFinal turno = buscarTurno(request.periodoId());
        validarMateriaDeLaCarrera(turno.carreraId(), request.materiaId());
        validarHorario(turno, request.materiaId(), request.fecha(), request.horaInicio(), request.horaFin(), null);
        validarAula(request.aulaId(), request.fecha(), request.horaInicio(), request.horaFin(), null);
        validarDocentes(request.docenteIds());

        ExamenFinal mesa = new ExamenFinal(request.materiaId(), turno.id(), request.fecha(), request.horaInicio(),
                request.horaFin(), request.aulaId());
        mesa.cambiarDocentes(request.docenteIds());
        examenFinalRepository.save(mesa);
        return toResponses(List.of(mesa), turno.carreraId()).getFirst();
    }

    /** Con inscriptos también se puede reprogramar: la mesa sigue siendo la misma. */
    @Transactional
    public ExamenFinalResponse modificar(Long examenFinalId, ModificacionExamenFinalRequest request) {
        ExamenFinal mesa = buscar(examenFinalId);
        PeriodoDeFinal turno = buscarTurno(mesa.getPeriodoId());
        validarHorario(turno, mesa.getMateriaId(), request.fecha(), request.horaInicio(), request.horaFin(), mesa.getId());
        validarAula(request.aulaId(), request.fecha(), request.horaInicio(), request.horaFin(), mesa.getId());
        validarDocentes(request.docenteIds());

        mesa.reprogramar(request.fecha(), request.horaInicio(), request.horaFin());
        mesa.setAulaId(request.aulaId());
        mesa.cambiarDocentes(request.docenteIds());
        return toResponses(List.of(mesa), turno.carreraId()).getFirst();
    }

    /** Borrado real, para una mesa mal cargada. Con inscriptos no se puede: la FK RESTRICT lo impide. */
    @Transactional
    public void eliminar(Long examenFinalId) {
        examenFinalRepository.delete(buscar(examenFinalId));
        try {
            examenFinalRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("La mesa de examen " + examenFinalId
                    + " tiene inscriptos: no se puede borrar, sólo reprogramar");
        }
    }

    private ExamenFinal buscar(Long examenFinalId) {
        return examenFinalRepository.findById(examenFinalId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la mesa de examen con id " + examenFinalId));
    }

    private PeriodoDeFinal buscarTurno(Long periodoId) {
        return periodosDeFinal.buscar(periodoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el turno de finales con id " + periodoId));
    }

    // El turno es de una carrera: la materia tiene que estar en alguno de sus planes, vigente o no.
    private void validarMateriaDeLaCarrera(Long carreraId, Long materiaId) {
        if (!nombresDeMaterias(carreraId).containsKey(materiaId)) {
            throw new ReglaNegocioException("La materia " + materiaId + " no es de la carrera del turno");
        }
    }

    /**
     * La mesa se toma después de que cierra la inscripción: si no, un estudiante podría anotarse a un
     * examen que ya pasó. Y no puede haber dos mesas de la misma materia a la misma hora (UNIQUE de la tabla).
     */
    private void validarHorario(PeriodoDeFinal turno, Long materiaId, LocalDate fecha, LocalTime horaInicio,
                                LocalTime horaFin, Long excluirExamenFinalId) {
        if (fecha.atTime(horaInicio).isBefore(turno.fechaCierre())) {
            throw new ReglaNegocioException("La mesa tiene que ser después del cierre de la inscripción del turno ("
                    + turno.fechaCierre().format(FECHA_HORA) + ")");
        }
        if (examenFinalRepository.horarioEnUso(materiaId, fecha, horaInicio, excluirExamenFinalId)) {
            throw new RecursoDuplicadoException("Ya hay una mesa de esta materia el "
                    + fecha.atTime(horaInicio).format(FECHA_HORA));
        }
    }

    // Sin aula, la mesa es virtual. El aula tiene que existir, estar activa y no tener otra mesa a esa hora.
    private void validarAula(Long aulaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                             Long excluirExamenFinalId) {
        if (aulaId == null) {
            return;
        }
        AulaResumen aula = comisionesApi.buscarAulas(List.of(aulaId)).stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el aula con id " + aulaId));
        if (!aula.activa()) {
            throw new ReglaNegocioException("El aula " + aula.nombre() + " está dada de baja");
        }
        if (examenFinalRepository.aulaOcupada(aulaId, fecha, horaInicio, horaFin, excluirExamenFinalId)) {
            throw new ReglaNegocioException("El aula " + aula.nombre() + " ya tiene otra mesa el "
                    + fecha.atTime(horaInicio).format(FECHA_HORA));
        }
    }

    // Sin repetidos, y todos docentes activos.
    private void validarDocentes(List<Long> docenteIds) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : docenteIds) {
            if (!ids.add(id)) {
                throw new ReglaNegocioException("El docente " + id + " está más de una vez");
            }
        }
        Map<Long, DocenteResumen> docentes = buscarDocentes(ids);
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
    }

    // Los nombres de otros módulos se piden una sola vez, para todas las mesas.
    private List<ExamenFinalResponse> toResponses(List<ExamenFinal> mesas, Long carreraId) {
        Map<Long, String> nombresMaterias = nombresDeMaterias(carreraId);
        Map<Long, AulaResumen> aulas = comisionesApi.buscarAulas(mesas.stream()
                        .map(ExamenFinal::getAulaId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(AulaResumen::id, Function.identity()));
        Map<Long, DocenteResumen> docentes = buscarDocentes(mesas.stream()
                .flatMap(mesa -> mesa.getDocenteIds().stream())
                .collect(Collectors.toSet()));
        return mesas.stream()
                .map(mesa -> examenFinalMapper.toResponse(mesa, nombresMaterias, aulas, docentes))
                .toList();
    }

    // De todos los planes de la carrera: una materia puede estar en varios con el mismo nombre.
    private Map<Long, String> nombresDeMaterias(Long carreraId) {
        Map<Long, String> nombres = new HashMap<>();
        for (Long planId : carrerasApi.idsDePlanes(carreraId)) {
            carrerasApi.materiasDelPlan(planId).forEach(materia -> nombres.put(materia.materiaId(), materia.nombre()));
        }
        return nombres;
    }

    private Map<Long, DocenteResumen> buscarDocentes(Set<Long> ids) {
        return personasApi.buscarDocentes(ids).stream()
                .collect(Collectors.toMap(DocenteResumen::personaId, Function.identity()));
    }
}
