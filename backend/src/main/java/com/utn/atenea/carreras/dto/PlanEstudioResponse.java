package com.utn.atenea.carreras.dto;

/** Fila del listado de planes de una carrera. */
public record PlanEstudioResponse(
        Long id,
        String nombre,
        boolean vigente
) {
}
