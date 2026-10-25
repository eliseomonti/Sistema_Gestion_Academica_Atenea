package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.model.Turno;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Lo único editable del grupo. Plan, año de la carrera y año lectivo quedan fijos: cambiarlos
 * dejaría materias de otro año o horarios sin controlar. Si están mal, se borra y se crea de nuevo.
 */
public record ModificacionComisionRequest(

        @NotNull(message = "El número de comisión es obligatorio")
        @Positive(message = "El número de comisión tiene que ser mayor a 0")
        Integer nroComision,

        @NotNull(message = "El turno es obligatorio")
        Turno turno
) {
}
