package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.model.DiaSemana;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Una franja semanal de la materia. Sin aulaId, la cursada de esa franja es virtual. */
public record HorarioRequest(

        @NotNull(message = "El día es obligatorio")
        DiaSemana diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        Long aulaId
) {

    // Si falta alguna de las dos horas no se evalúa: ya lo informa su @NotNull.
    @AssertTrue(message = "La hora de fin tiene que ser posterior a la de inicio")
    public boolean isHoraFinPosterior() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
