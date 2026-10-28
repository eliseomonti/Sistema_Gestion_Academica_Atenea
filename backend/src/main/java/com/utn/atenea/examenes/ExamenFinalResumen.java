package com.utn.atenea.examenes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** Lo que otro módulo necesita saber de una mesa de examen final. aulaId en null: la mesa es virtual. */
public record ExamenFinalResumen(
        Long id,
        Long materiaId,
        Long periodoId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long aulaId,
        Set<Long> docenteIds
) {
}
