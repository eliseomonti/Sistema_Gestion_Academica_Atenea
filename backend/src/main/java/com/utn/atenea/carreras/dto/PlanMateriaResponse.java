package com.utn.atenea.carreras.dto;

import com.utn.atenea.carreras.PeriodoLectivo;

/** Opción del desplegable de materias de un plan, al armar una comisión. */
public record PlanMateriaResponse(
        Long materiaId,
        String codigo,
        String nombre,
        int anioCursado,
        PeriodoLectivo periodoLectivo
) {
}
