package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.model.CondicionExamen;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * La nota de una inscripción: a la materia de la comisión en una evaluación, o a la mesa en un final.
 * PRESENTE lleva nota; AUSENTE no.
 */
public record NotaRequest(

        @NotNull(message = "La inscripción es obligatoria")
        Long inscripcionId,

        @NotNull(message = "La condición es obligatoria")
        CondicionExamen condicion,

        @Min(value = 0, message = "La nota tiene que estar entre 0 y 10")
        @Max(value = 10, message = "La nota tiene que estar entre 0 y 10")
        Integer nota
) {

    // Sin condición no se evalúa: ya lo informa su @NotNull.
    @AssertTrue(message = "Un presente lleva nota y un ausente no")
    public boolean isNotaSegunCondicion() {
        return condicion == null || (condicion == CondicionExamen.PRESENTE) == (nota != null);
    }
}
