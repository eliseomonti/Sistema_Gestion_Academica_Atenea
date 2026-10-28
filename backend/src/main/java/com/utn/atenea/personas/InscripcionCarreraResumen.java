package com.utn.atenea.personas;

/** Lo que otro módulo necesita saber de la inscripción de un estudiante a un plan para anotarlo a algo. */
public record InscripcionCarreraResumen(Long id, Long estudianteId, Long planEstudioId, boolean activa) {
}
