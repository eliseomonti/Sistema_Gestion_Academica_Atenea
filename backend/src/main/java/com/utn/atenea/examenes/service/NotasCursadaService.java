package com.utn.atenea.examenes.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.comisiones.ComisionMateriaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.examenes.InscriptoACursada;
import com.utn.atenea.examenes.Inscriptos;
import com.utn.atenea.examenes.dto.CierreCursadaEstudianteResponse;
import com.utn.atenea.examenes.dto.CursadaEstudianteResponse;
import com.utn.atenea.examenes.dto.NotaEvaluacionEstudianteResponse;
import com.utn.atenea.examenes.model.CierreCursada;
import com.utn.atenea.examenes.model.EvaluacionCursada;
import com.utn.atenea.examenes.model.NotaEvaluacion;
import com.utn.atenea.examenes.repository.CierreCursadaRepository;
import com.utn.atenea.examenes.repository.EvaluacionCursadaRepository;
import com.utn.atenea.examenes.repository.NotaEvaluacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Autoservicio del estudiante: las notas de sus cursadas, con todas las evaluaciones y el acta de cierre.
 * Las cursadas las da el puerto Inscriptos; las notas son las vigentes. estudianteId es el personaId del token.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotasCursadaService {

    private final Inscriptos inscriptos;
    private final EvaluacionCursadaRepository evaluacionCursadaRepository;
    private final NotaEvaluacionRepository notaEvaluacionRepository;
    private final CierreCursadaRepository cierreCursadaRepository;
    private final ComisionesApi comisionesApi;
    private final CarrerasApi carrerasApi;

    /** De todos los años, del más reciente al más viejo; dentro del año, por período y materia. */
    public List<CursadaEstudianteResponse> listar(Long estudianteId) {
        List<InscriptoACursada> cursadas = inscriptos.cursadasDelEstudiante(estudianteId);
        if (cursadas.isEmpty()) {
            return List.of();
        }
        List<Long> inscripcionIds = cursadas.stream().map(InscriptoACursada::inscripcionComisionId).toList();
        List<Long> comisionMateriaIds = cursadas.stream().map(InscriptoACursada::comisionMateriaId).toList();

        // Todo se pide una sola vez, para todas las cursadas.
        Map<Long, ComisionMateriaResumen> comisionesMateria = comisionesApi.buscarComisionesMateria(comisionMateriaIds)
                .stream()
                .collect(Collectors.toMap(ComisionMateriaResumen::id, Function.identity()));
        Map<Long, List<EvaluacionCursada>> evaluacionesPorComisionMateria = evaluacionCursadaRepository
                .findByComisionMateriaIdInOrderByFechaAscIdAsc(comisionMateriaIds).stream()
                .collect(Collectors.groupingBy(EvaluacionCursada::getComisionMateriaId));
        Map<Long, Map<Long, NotaEvaluacion>> notasPorInscripcion = notaEvaluacionRepository.vigentes(inscripcionIds)
                .stream()
                .collect(Collectors.groupingBy(NotaEvaluacion::getInscripcionComisionId,
                        Collectors.toMap(nota -> nota.getEvaluacion().getId(), Function.identity())));
        Map<Long, CierreCursada> cierres = cierreCursadaRepository.vigentes(inscripcionIds).stream()
                .collect(Collectors.toMap(CierreCursada::getInscripcionComisionId, Function.identity()));
        Map<Long, Map<Long, String>> nombresPorPlan = new HashMap<>();

        return cursadas.stream()
                .map(cursada -> {
                    ComisionMateriaResumen comisionMateria = comisionesMateria.get(cursada.comisionMateriaId());
                    String nombreMateria = nombresPorPlan
                            .computeIfAbsent(comisionMateria.planEstudioId(), this::nombresDeMaterias)
                            .get(comisionMateria.materiaId());
                    Map<Long, NotaEvaluacion> notas = notasPorInscripcion
                            .getOrDefault(cursada.inscripcionComisionId(), Map.of());
                    List<NotaEvaluacionEstudianteResponse> evaluaciones = evaluacionesPorComisionMateria
                            .getOrDefault(cursada.comisionMateriaId(), List.of()).stream()
                            .map(evaluacion -> toResponse(evaluacion, notas.get(evaluacion.getId())))
                            .toList();
                    return new CursadaEstudianteResponse(cursada.inscripcionComisionId(), comisionMateria.materiaId(),
                            nombreMateria, comisionMateria.nombreComision(), comisionMateria.anioLectivo(),
                            comisionMateria.periodoLectivo(), evaluaciones,
                            toResponse(cierres.get(cursada.inscripcionComisionId())));
                })
                .sorted(Comparator.comparingInt(CursadaEstudianteResponse::anioLectivo).reversed()
                        .thenComparing(CursadaEstudianteResponse::periodoLectivo)
                        .thenComparing(CursadaEstudianteResponse::nombreMateria))
                .toList();
    }

    // nota en null: todavía no está cargada.
    private static NotaEvaluacionEstudianteResponse toResponse(EvaluacionCursada evaluacion, NotaEvaluacion nota) {
        return new NotaEvaluacionEstudianteResponse(evaluacion.getId(), evaluacion.getNombre(), evaluacion.getTipo(),
                evaluacion.getFecha(),
                evaluacion.esRecuperatorio() ? evaluacion.getEvaluacionRecuperada().getId() : null,
                nota != null ? nota.getCondicion() : null,
                nota != null ? nota.getNota() : null);
    }

    // cierre en null: la cursada todavía no se cerró.
    private static CierreCursadaEstudianteResponse toResponse(CierreCursada cierre) {
        if (cierre == null) {
            return null;
        }
        return new CierreCursadaEstudianteResponse(cierre.getCondicion(), cierre.getNotaCursada(),
                cierre.getFechaCierre(), cierre.getFechaVencimiento());
    }

    private Map<Long, String> nombresDeMaterias(Long planEstudioId) {
        return carrerasApi.materiasDelPlan(planEstudioId).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
    }
}
