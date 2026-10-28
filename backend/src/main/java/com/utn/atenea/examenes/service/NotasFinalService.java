package com.utn.atenea.examenes.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.examenes.InscriptoAFinal;
import com.utn.atenea.examenes.Inscriptos;
import com.utn.atenea.examenes.dto.FinalEstudianteResponse;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.examenes.model.NotaFinal;
import com.utn.atenea.examenes.repository.ExamenFinalRepository;
import com.utn.atenea.examenes.repository.NotaFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Autoservicio del estudiante: las notas de los finales que rindió. Las inscripciones las da el puerto
 * Inscriptos; las notas son las vigentes. estudianteId es el personaId del token.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotasFinalService {

    private final Inscriptos inscriptos;
    private final ExamenFinalRepository examenFinalRepository;
    private final NotaFinalRepository notaFinalRepository;
    private final CarrerasApi carrerasApi;

    /**
     * Las mesas que ya se tomaron (hasta hoy inclusive), con o sin nota cargada, de la más reciente a la
     * más vieja. Las que vienen están en las inscripciones a final.
     */
    public List<FinalEstudianteResponse> listar(Long estudianteId) {
        List<InscriptoAFinal> finales = inscriptos.finalesDelEstudiante(estudianteId);
        if (finales.isEmpty()) {
            return List.of();
        }
        Map<Long, ExamenFinal> mesas = examenFinalRepository
                .findAllById(finales.stream().map(InscriptoAFinal::examenFinalId).toList()).stream()
                .collect(Collectors.toMap(ExamenFinal::getId, Function.identity()));
        Map<Long, NotaFinal> notas = notaFinalRepository
                .vigentes(finales.stream().map(InscriptoAFinal::inscripcionExamenFinalId).toList()).stream()
                .collect(Collectors.toMap(NotaFinal::getInscripcionExamenFinalId, Function.identity()));
        Map<Long, Map<Long, String>> nombresPorPlan = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        return finales.stream()
                .filter(rendido -> !mesas.get(rendido.examenFinalId()).getFecha().isAfter(hoy))
                .map(rendido -> {
                    ExamenFinal mesa = mesas.get(rendido.examenFinalId());
                    NotaFinal nota = notas.get(rendido.inscripcionExamenFinalId());
                    String nombreMateria = nombresPorPlan
                            .computeIfAbsent(rendido.planEstudioId(), this::nombresDeMaterias)
                            .get(mesa.getMateriaId());
                    return new FinalEstudianteResponse(rendido.inscripcionExamenFinalId(), mesa.getId(),
                            mesa.getMateriaId(), nombreMateria, mesa.getFecha(),
                            nota != null ? nota.getCondicion() : null,
                            nota != null ? nota.getNota() : null,
                            nota != null ? nota.estaAprobada() : null);
                })
                .sorted(Comparator.comparing(FinalEstudianteResponse::fecha).reversed()
                        .thenComparing(FinalEstudianteResponse::nombreMateria))
                .toList();
    }

    private Map<Long, String> nombresDeMaterias(Long planEstudioId) {
        return carrerasApi.materiasDelPlan(planEstudioId).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
    }
}
