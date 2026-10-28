package com.utn.atenea.examenes.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Una mesa de examen final dentro de un turno. Sin aulaId, la mesa es virtual. */
public record AltaExamenFinalRequest(

        @NotNull(message = "El turno es obligatorio")
        Long periodoId,

        @NotNull(message = "La materia es obligatoria")
        Long materiaId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        Long aulaId,

        @NotEmpty(message = "El tribunal tiene que tener al menos un docente")
        List<@NotNull(message = "El docente es obligatorio") Long> docenteIds
) {

    // Si falta alguna de las dos horas no se evalúa: ya lo informa su @NotNull.
    @AssertTrue(message = "La hora de fin tiene que ser posterior a la de inicio")
    public boolean isHoraFinPosterior() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
