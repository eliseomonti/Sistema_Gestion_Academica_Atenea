package com.utn.atenea.personas;

/** Lo que otro módulo necesita saber de un estudiante para listarlo, por ejemplo en la planilla de un docente. */
public record EstudianteResumen(Long personaId, String nombre, String apellido, Long legajoEstudiante, String email) {
}
