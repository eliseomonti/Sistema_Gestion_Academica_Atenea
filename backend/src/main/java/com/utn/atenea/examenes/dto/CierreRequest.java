package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.CondicionCursada;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cómo terminó la cursada de una inscripción. La nota es la de cursada: obligatoria en PROMOCIONADO,
 * opcional en las otras (el que abandonó no tiene). Las fechas las pone el sistema.
 */
public record CierreRequest(

        @NotNull(message = "La inscripción es obligatoria")
        Long inscripcionComisionId,

        @NotNull(message = "La condición es obligatoria")
        CondicionCursada condicion,

        @Min(value = 0, message = "La nota tiene que estar entre 0 y 10")
        @Max(value = 10, message = "La nota tiene que estar entre 0 y 10")
        Integer nota
) {

    @AssertTrue(message = "La promoción lleva nota")
    public boolean isNotaSiPromociona() {
        return condicion != CondicionCursada.PROMOCIONADO || nota != null;
    }
}
