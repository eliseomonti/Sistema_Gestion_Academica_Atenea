package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.model.CondicionExamen;
import com.utn.atenea.examenes.model.TipoEvaluacion;

import java.time.LocalDate;

/**
 * Una evaluación de la cursada con la nota vigente del estudiante. condicion y nota en null: todavía no
 * está cargada; nota en null con AUSENTE: no se presentó. evaluacionRecuperadaId, sólo si es un recuperatorio.
 */
public record NotaEvaluacionEstudianteResponse(
        Long evaluacionId,
        String nombre,
        TipoEvaluacion tipo,
        LocalDate fecha,
        Long evaluacionRecuperadaId,
        CondicionExamen condicion,
        Integer nota
) {
}
