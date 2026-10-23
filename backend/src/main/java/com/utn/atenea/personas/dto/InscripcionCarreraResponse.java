package com.utn.atenea.personas.dto;

import com.utn.atenea.personas.model.EstadoInscripcionCarrera;

import java.time.LocalDate;

public record InscripcionCarreraResponse(
        Long id,
        Long personaId,
        Long legajoAlumno,
        Long planEstudioId,
        EstadoInscripcionCarrera estado,
        LocalDate fechaInscripcion
) {
}
