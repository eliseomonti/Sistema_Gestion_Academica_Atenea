package com.utn.atenea.personas.dto;

import com.utn.atenea.personas.model.EstadoInscripcionCarrera;

import java.time.LocalDate;

public record InscripcionCarreraDetalleResponse(
        Long id,
        String nombreCarrera,
        String nombrePlan,
        EstadoInscripcionCarrera estado,
        LocalDate fechaInscripcion,
        LocalDate fechaEstado
) {
}
