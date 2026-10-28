package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.inscripciones.model.TipoPeriodo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** periodoLectivo sólo va en los de CURSADA: dice a qué cuatrimestre corresponde la ventana. */
public record AltaPeriodoInscripcionRequest(

        @NotNull(message = "La carrera es obligatoria")
        Long carreraId,

        @NotNull(message = "El tipo es obligatorio")
        TipoPeriodo tipo,

        @NotNull(message = "El año lectivo es obligatorio")
        @Min(value = 2000, message = "El año lectivo tiene que estar entre 2000 y 2100")
        @Max(value = 2100, message = "El año lectivo tiene que estar entre 2000 y 2100")
        Integer anioLectivo,

        PeriodoLectivo periodoLectivo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime fechaInicio,

        @NotNull(message = "La fecha de cierre es obligatoria")
        LocalDateTime fechaCierre
) {

    // Sin tipo no se evalúa: ya lo informa su @NotNull.
    @AssertTrue(message = "El período lectivo es obligatorio en las inscripciones a cursada y no va en las de final")
    public boolean isPeriodoLectivoSegunTipo() {
        return tipo == null || (tipo == TipoPeriodo.CURSADA) == (periodoLectivo != null);
    }

    // Si falta alguna de las dos fechas no se evalúa: ya lo informa su @NotNull.
    @AssertTrue(message = "La fecha de cierre tiene que ser posterior a la de inicio")
    public boolean isFechaCierrePosterior() {
        return fechaInicio == null || fechaCierre == null || fechaCierre.isAfter(fechaInicio);
    }
}
