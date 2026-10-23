package com.utn.atenea.personas.dto;

import jakarta.validation.constraints.NotNull;

public record InscripcionCarreraRequest(

        @NotNull(message = "El plan de estudio es obligatorio")
        Long planEstudioId
) {
}
