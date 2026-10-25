package com.utn.atenea.personas.dto;

/** Fila del listado de estudiantes del admin. La carrera es un filtro, no una columna. */
public record EstudianteListadoResponse(
        Long personaId,
        Long legajo,
        String apellido,
        String nombre,
        String dni,
        String email
) {
}
