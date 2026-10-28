package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.model.TipoEvaluacion;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Una evaluación de la materia de la comisión. Con evaluacionRecuperadaId es el recuperatorio de esa:
 * entonces el tipo no va, se copia de la que recupera.
 */
public record AltaEvaluacionRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre puede tener hasta 50 caracteres")
        String nombre,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        TipoEvaluacion tipo,

        Long evaluacionRecuperadaId
) {

    @AssertTrue(message = "El tipo es obligatorio en una evaluación original y no va en un recuperatorio")
    public boolean isTipoSegunRecuperatorio() {
        return (evaluacionRecuperadaId == null) == (tipo != null);
    }
}
