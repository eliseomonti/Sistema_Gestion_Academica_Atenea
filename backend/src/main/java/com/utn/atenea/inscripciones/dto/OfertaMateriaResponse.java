package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;

import java.util.List;

/**
 * Una materia que el estudiante puede cursar en el período abierto, con las comisiones en que se dicta.
 * planEstudioId distingue la oferta de cada carrera si está inscripto a más de una.
 */
public record OfertaMateriaResponse(
        Long planEstudioId,
        Long materiaId,
        String nombreMateria,
        int anioCursado,
        PeriodoLectivo periodoLectivo,
        List<OfertaComisionResponse> comisiones
) {
}
