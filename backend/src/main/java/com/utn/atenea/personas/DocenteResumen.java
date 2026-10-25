package com.utn.atenea.personas;

/** Lo que otro módulo necesita saber de un docente para asignarlo a una comisión y mostrarlo. */
public record DocenteResumen(Long personaId, String nombre, String apellido, Long legajoDocente, boolean activo) {
}
