package com.utn.atenea.examenes.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Una mesa en cuyo tribunal está el docente. nombreAula en null: la mesa es virtual. */
public record MesaDocenteResponse(
        Long examenFinalId,
        Long materiaId,
        String nombreMateria,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String nombreAula
) {
}
