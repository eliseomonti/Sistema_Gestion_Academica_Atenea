package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.inscripciones.model.EstadoInscripcion;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Fila de "mis inscripciones a final"; también es la respuesta de la inscripción y de la baja.
 * nombreAula en null: la mesa es virtual.
 */
public record InscripcionFinalResponse(
        Long id,
        Long examenFinalId,
        Long materiaId,
        String nombreMateria,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String nombreAula,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado
) {
}
