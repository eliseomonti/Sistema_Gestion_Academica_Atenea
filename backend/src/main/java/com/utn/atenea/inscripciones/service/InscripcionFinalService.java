package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.comisiones.AulaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.ExamenFinalResumen;
import com.utn.atenea.examenes.ExamenesApi;
import com.utn.atenea.inscripciones.dto.InscripcionFinalResponse;
import com.utn.atenea.inscripciones.mapper.InscripcionFinalMapper;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionFinal;
import com.utn.atenea.inscripciones.repository.InscripcionFinalRepository;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Autoservicio del estudiante: ver sus inscripciones a final y darse de baja. La inscripción está en
 * AltaInscripcionFinalService. estudianteId es el personaId del token, nunca viene del request.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InscripcionFinalService {

    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final PersonasApi personasApi;
    private final ExamenesApi examenesApi;
    private final CarrerasApi carrerasApi;
    private final ComisionesApi comisionesApi;
    private final InscripcionFinalMapper inscripcionFinalMapper;

    /** Las inscripciones vigentes del estudiante a mesas que todavía no se tomaron, por fecha y hora. */
    public List<InscripcionFinalResponse> listar(Long estudianteId) {
        Map<Long, Long> planPorInscripcionCarrera = planPorInscripcionCarrera(estudianteId);
        if (planPorInscripcionCarrera.isEmpty()) {
            return List.of();
        }
        List<InscripcionFinal> inscripciones = inscripcionFinalRepository
                .findByInscripcionCarreraIdInAndEstado(planPorInscripcionCarrera.keySet(), EstadoInscripcion.INSCRIPTO);
        LocalDate hoy = LocalDate.now();
        return toResponses(inscripciones, planPorInscripcionCarrera).stream()
                .filter(inscripcion -> !inscripcion.fecha().isBefore(hoy))
                .sorted(Comparator.comparing(InscripcionFinalResponse::fecha)
                        .thenComparing(InscripcionFinalResponse::horaInicio))
                .toList();
    }

    /**
     * El estudiante se puede dar de baja hasta el día anterior a la mesa, aunque el turno ya haya cerrado.
     * Una inscripción en BAJA no puede tener nota: el docente sólo carga las de los INSCRIPTO.
     */
    @Transactional
    public InscripcionFinalResponse darDeBaja(Long estudianteId, Long inscripcionId) {
        Map<Long, Long> planPorInscripcionCarrera = planPorInscripcionCarrera(estudianteId);
        // Con las inscripciones a carrera del estudiante en la condición, la de otro da 404 y no 403:
        // no se revela que existe.
        InscripcionFinal inscripcion = inscripcionFinalRepository
                .findByIdAndInscripcionCarreraIdIn(inscripcionId, planPorInscripcionCarrera.keySet())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la inscripción a final con id " + inscripcionId));
        if (inscripcion.getEstado() == EstadoInscripcion.BAJA) {
            throw new ReglaNegocioException("La inscripción a final " + inscripcionId + " ya está dada de baja");
        }
        ExamenFinalResumen mesa = examenesApi.buscarExamenesFinales(List.of(inscripcion.getExamenFinalId())).getFirst();
        LocalDate hoy = LocalDate.now();
        if (!hoy.isBefore(mesa.fecha())) {
            throw new ReglaNegocioException("La baja de un final se hace hasta el día anterior a la mesa");
        }

        inscripcion.darDeBaja(hoy);
        return toResponses(List.of(inscripcion), planPorInscripcionCarrera).getFirst();
    }

    /**
     * Lo que inscripciones no guarda se pide una sola vez para todas: la mesa a examenes, el aula a
     * comisiones y el nombre de la materia a carreras, en el plan de cada inscripción a carrera.
     * También lo usa AltaInscripcionFinalService.
     */
    List<InscripcionFinalResponse> toResponses(List<InscripcionFinal> inscripciones,
                                               Map<Long, Long> planPorInscripcionCarrera) {
        if (inscripciones.isEmpty()) {
            return List.of();
        }
        Map<Long, ExamenFinalResumen> mesas = examenesApi
                .buscarExamenesFinales(inscripciones.stream().map(InscripcionFinal::getExamenFinalId).toList())
                .stream()
                .collect(Collectors.toMap(ExamenFinalResumen::id, Function.identity()));
        Map<Long, String> aulas = comisionesApi
                .buscarAulas(mesas.values().stream().map(ExamenFinalResumen::aulaId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(AulaResumen::id, AulaResumen::nombre));
        Map<Long, Map<Long, String>> nombresPorPlan = new HashMap<>();

        return inscripciones.stream()
                .map(inscripcion -> {
                    ExamenFinalResumen mesa = mesas.get(inscripcion.getExamenFinalId());
                    String nombreMateria = nombresPorPlan
                            .computeIfAbsent(planPorInscripcionCarrera.get(inscripcion.getInscripcionCarreraId()),
                                    this::nombresDeMaterias)
                            .get(mesa.materiaId());
                    String nombreAula = mesa.aulaId() != null ? aulas.get(mesa.aulaId()) : null;
                    return inscripcionFinalMapper.toResponse(inscripcion, mesa, nombreMateria, nombreAula);
                })
                .toList();
    }

    // Todas las inscripciones a carrera, también las inactivas: la baja y el listado son de lo ya hecho.
    private Map<Long, Long> planPorInscripcionCarrera(Long estudianteId) {
        return personasApi.inscripcionesCarrera(estudianteId).stream()
                .collect(Collectors.toMap(InscripcionCarreraResumen::id, InscripcionCarreraResumen::planEstudioId));
    }

    private Map<Long, String> nombresDeMaterias(Long planEstudioId) {
        return carrerasApi.materiasDelPlan(planEstudioId).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
    }
}
