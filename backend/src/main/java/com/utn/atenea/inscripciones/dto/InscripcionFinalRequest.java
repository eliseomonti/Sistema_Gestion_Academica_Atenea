package com.utn.atenea.inscripciones.dto;

import jakarta.validation.constraints.NotNull;

/** El estudiante sale del token: el request sólo dice a qué mesa se anota. */
public record InscripcionFinalRequest(

        @NotNull(message = "La mesa de examen es obligatoria")
        Long examenFinalId
) {
}
