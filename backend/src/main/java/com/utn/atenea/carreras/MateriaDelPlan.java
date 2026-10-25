package com.utn.atenea.carreras;

/** Lo que otro módulo necesita saber de una materia dentro de un plan para dictarla en una comisión. */
public record MateriaDelPlan(Long materiaId, String nombre, int anioCursado, PeriodoLectivo periodoLectivo) {
}
