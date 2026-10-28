package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.model.TipoEvaluacion;

import java.time.LocalDate;

/** evaluacionRecuperadaId, sólo si es un recuperatorio. */
public record EvaluacionResponse(
        Long id,
        Long comisionMateriaId,
        String nombre,
        TipoEvaluacion tipo,
        LocalDate fecha,
        Long evaluacionRecuperadaId
) {
}
