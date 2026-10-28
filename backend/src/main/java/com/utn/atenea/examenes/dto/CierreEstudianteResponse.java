package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.CondicionCursada;

import java.time.LocalDate;

/**
 * Una fila del acta: el estudiante y su acta de cierre vigente. condicion y fechas en null: todavía no
 * se cerró. fechaVencimiento, sólo si es REGULAR.
 */
public record CierreEstudianteResponse(
        Long inscripcionComisionId,
        Long personaId,
        Long legajo,
        String apellido,
        String nombre,
        CondicionCursada condicion,
        Integer nota,
        LocalDate fechaCierre,
        LocalDate fechaVencimiento
) {
}
