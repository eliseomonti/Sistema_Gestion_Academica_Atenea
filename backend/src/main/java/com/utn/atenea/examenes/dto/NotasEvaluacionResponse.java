package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.model.TipoEvaluacion;

import java.time.LocalDate;
import java.util.List;

/** La planilla de una evaluación: todos los anotados a la materia de la comisión, por apellido. */
public record NotasEvaluacionResponse(
        Long evaluacionId,
        String nombre,
        TipoEvaluacion tipo,
        LocalDate fecha,
        List<NotaEstudianteResponse> notas
) {
}
