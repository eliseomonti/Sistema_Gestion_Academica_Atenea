package com.utn.atenea.examenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** El tipo y la evaluación que recupera no cambian: para eso se borra y se crea otra. */
public record ModificacionEvaluacionRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre puede tener hasta 50 caracteres")
        String nombre,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha
) {
}
