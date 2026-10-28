package com.utn.atenea.inscripciones.dto;

import jakarta.validation.constraints.NotNull;

/** El estudiante sale del token: el request sólo dice a qué materia de qué comisión se anota. */
public record InscripcionComisionRequest(

        @NotNull(message = "La materia de la comisión es obligatoria")
        Long comisionMateriaId
) {
}
