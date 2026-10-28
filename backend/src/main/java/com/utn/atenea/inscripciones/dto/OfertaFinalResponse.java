package com.utn.atenea.inscripciones.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Una mesa a la que el estudiante se puede anotar en un turno abierto. planEstudioId distingue la
 * oferta de cada carrera si está inscripto a más de una; inscripto dice si ya está anotado en esta.
 * nombreAula en null: la mesa es virtual.
 */
public record OfertaFinalResponse(
        Long planEstudioId,
        Long examenFinalId,
        Long materiaId,
        String nombreMateria,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String nombreAula,
        List<DocenteMesaResponse> docentes,
        boolean inscripto
) {
}
