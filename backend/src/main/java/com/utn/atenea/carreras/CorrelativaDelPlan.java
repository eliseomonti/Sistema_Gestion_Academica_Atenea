package com.utn.atenea.carreras;

/**
 * Una correlatividad del plan, por materia y no por plan_materia: para cursar o rendir materiaId hay que
 * tener correlativaId en ese estado. Un estado en null es que no se exige para eso (nunca los dos).
 */
public record CorrelativaDelPlan(
        Long materiaId,
        Long correlativaId,
        EstadoCorrelativa paraCursar,
        EstadoCorrelativa paraRendir
) {
}
