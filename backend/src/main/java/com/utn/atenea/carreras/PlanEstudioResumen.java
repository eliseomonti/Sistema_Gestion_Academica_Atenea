package com.utn.atenea.carreras;

/** Lo que otro módulo necesita saber de un plan para inscribir a alguien y mostrarlo. */
public record PlanEstudioResumen(
        Long id,
        Long carreraId,
        String nombreCarrera,
        String nombre,
        boolean admiteInscripciones
) {
}
