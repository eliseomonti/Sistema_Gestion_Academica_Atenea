package com.utn.atenea.examenes.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Una mesa de examen final. aulaId y nombreAula en null: la mesa es virtual. */
public record ExamenFinalResponse(
        Long id,
        Long periodoId,
        Long materiaId,
        String nombreMateria,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long aulaId,
        String nombreAula,
        List<DocenteTribunalResponse> docentes
) {
}
