package com.utn.atenea.personas.dto;

/** Fila del listado de docentes del admin. */
public record DocenteListadoResponse(
        Long personaId,
        Long legajo,
        String apellido,
        String nombre,
        String dni,
        String email
) {
}
