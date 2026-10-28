package com.utn.atenea.examenes.dto;

import com.utn.atenea.carreras.PeriodoLectivo;

import java.util.List;

/**
 * Una cursada del estudiante con sus notas: todas las evaluaciones de la materia en la comisión, con o
 * sin nota cargada, y el acta de cierre (null si todavía no se cerró).
 */
public record CursadaEstudianteResponse(
        Long inscripcionComisionId,
        Long materiaId,
        String nombreMateria,
        String nombreComision,
        int anioLectivo,
        PeriodoLectivo periodoLectivo,
        List<NotaEvaluacionEstudianteResponse> evaluaciones,
        CierreCursadaEstudianteResponse cierre
) {
}
