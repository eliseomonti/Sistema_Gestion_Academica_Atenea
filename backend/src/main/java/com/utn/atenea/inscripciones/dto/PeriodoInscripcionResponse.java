package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.inscripciones.model.TipoPeriodo;

import java.time.LocalDateTime;

/** Fila del listado de períodos; también es la respuesta del detalle, el alta y la modificación. */
public record PeriodoInscripcionResponse(
        Long id,
        Long carreraId,
        TipoPeriodo tipo,
        int anioLectivo,
        PeriodoLectivo periodoLectivo,
        LocalDateTime fechaInicio,
        LocalDateTime fechaCierre,
        boolean habilitada
) {
}
