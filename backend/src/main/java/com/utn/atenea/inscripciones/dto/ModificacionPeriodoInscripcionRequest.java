package com.utn.atenea.inscripciones.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Lo único editable del período. Carrera, tipo, año y período lectivo quedan fijos: las inscripciones
 * ya hechas dependen de ellos. Si están mal, se borra y se crea de nuevo.
 */
public record ModificacionPeriodoInscripcionRequest(

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime fechaInicio,

        @NotNull(message = "La fecha de cierre es obligatoria")
        LocalDateTime fechaCierre
) {

    // Si falta alguna de las dos fechas no se evalúa: ya lo informa su @NotNull.
    @AssertTrue(message = "La fecha de cierre tiene que ser posterior a la de inicio")
    public boolean isFechaCierrePosterior() {
        return fechaInicio == null || fechaCierre == null || fechaCierre.isAfter(fechaInicio);
    }
}
