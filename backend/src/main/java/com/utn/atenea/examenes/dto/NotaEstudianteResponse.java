package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.model.CondicionExamen;

/**
 * Una fila de la planilla del docente: el estudiante y su nota vigente. condicion y nota en null:
 * todavía no está cargada. inscripcionId es el que se manda para cargarla.
 */
public record NotaEstudianteResponse(
        Long inscripcionId,
        Long personaId,
        Long legajo,
        String apellido,
        String nombre,
        CondicionExamen condicion,
        Integer nota
) {
}
