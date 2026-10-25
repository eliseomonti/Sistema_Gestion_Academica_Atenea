package com.utn.atenea.personas.dto;

/**
 * Persona encontrada por DNI. Angular la usa antes de un alta: si ya existe, en vez del
 * formulario completo ofrece asignarle el rol que le falta. Los legajos dicen qué roles tiene.
 */
public record PersonaResponse(
        Long id,
        String nombre,
        String apellido,
        String dni,
        String email,
        Long legajoEstudiante,
        Long legajoDocente,
        boolean activo
) {
}
