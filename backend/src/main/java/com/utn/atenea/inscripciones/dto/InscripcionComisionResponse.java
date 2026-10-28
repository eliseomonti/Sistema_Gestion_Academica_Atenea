package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;

import java.time.LocalDate;

/** Fila de "mis inscripciones a cursada"; también es la respuesta de la inscripción y de la baja. */
public record InscripcionComisionResponse(
        Long id,
        Long comisionMateriaId,
        String nombreComision,
        Long materiaId,
        String nombreMateria,
        PeriodoLectivo periodoLectivo,
        int anioLectivo,
        LocalDate fechaInscripcion,
        EstadoInscripcion estado
) {
}
