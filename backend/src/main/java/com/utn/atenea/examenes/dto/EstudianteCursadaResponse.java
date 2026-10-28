package com.utn.atenea.examenes.dto;

/** Un estudiante anotado a la materia de la comisión, para el listado del docente. */
public record EstudianteCursadaResponse(
        Long inscripcionComisionId,
        Long personaId,
        Long legajo,
        String apellido,
        String nombre,
        String email
) {
}
