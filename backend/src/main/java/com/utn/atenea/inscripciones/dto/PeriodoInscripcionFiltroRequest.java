package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.inscripciones.model.TipoPeriodo;
import jakarta.validation.constraints.NotNull;

/** Filtros del listado, como query params. Sólo la carrera es obligatoria; sin anioLectivo se toma el año actual. */
public record PeriodoInscripcionFiltroRequest(

        @NotNull(message = "La carrera es obligatoria")
        Long carreraId,

        Integer anioLectivo,
        TipoPeriodo tipo
) {
}
