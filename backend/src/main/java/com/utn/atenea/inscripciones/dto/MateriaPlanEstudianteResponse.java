package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.inscripciones.reglas.EstadoMateria;

import java.time.LocalDate;

/**
 * Una materia del plan con su estado para el estudiante. nota, sólo si está APROBADA (la del final o la
 * de la promoción); vencimientoRegularidad, sólo si está REGULAR.
 */
public record MateriaPlanEstudianteResponse(
        Long materiaId,
        String nombre,
        int anioCursado,
        PeriodoLectivo periodoLectivo,
        EstadoMateria estado,
        Integer nota,
        LocalDate vencimientoRegularidad
) {
}
