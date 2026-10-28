package com.utn.atenea.comisiones;

import com.utn.atenea.carreras.PeriodoLectivo;

/**
 * Lo que otro módulo necesita saber de una materia de una comisión para inscribir a alguien y
 * mostrarlo. nombreComision es el de la planilla ("1/25"); cupoMaximo en null es sin límite.
 */
public record ComisionMateriaResumen(
        Long id,
        Long comisionId,
        String nombreComision,
        Long planEstudioId,
        int anioLectivo,
        Long materiaId,
        PeriodoLectivo periodoLectivo,
        Integer cupoMaximo,
        boolean comisionActiva
) {
}
