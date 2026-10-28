package com.utn.atenea.examenes.dto;

import jakarta.validation.constraints.NotNull;

/** Filtros del listado, como query params: las mesas se listan por turno. */
public record ExamenFinalFiltroRequest(

        @NotNull(message = "El turno es obligatorio")
        Long periodoId
) {
}
