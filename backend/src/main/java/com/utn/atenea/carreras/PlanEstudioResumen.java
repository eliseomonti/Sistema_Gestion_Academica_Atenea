package com.utn.atenea.carreras;

/** Lo que otro módulo necesita saber de un plan para inscribir a alguien. */
public record PlanEstudioResumen(Long id, Long carreraId, boolean admiteInscripciones) {
}
