package com.utn.atenea.personas.dto;

/** Fila del listado de alumnos del admin. La carrera es un filtro, no una columna. */
public record AlumnoListadoResponse(
        Long personaId,
        Long legajo,
        String apellido,
        String nombre,
        String dni,
        String email
) {
}
