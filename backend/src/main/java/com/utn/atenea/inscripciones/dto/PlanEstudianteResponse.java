package com.utn.atenea.inscripciones.dto;

import java.util.List;

/**
 * El plan de estudio de una inscripción a carrera del estudiante, con el estado de cada materia.
 * porcentajeAvance es materiasAprobadas sobre totalMaterias, redondeado para abajo.
 */
public record PlanEstudianteResponse(
        Long planEstudioId,
        String nombreCarrera,
        String nombrePlan,
        int materiasAprobadas,
        int totalMaterias,
        int porcentajeAvance,
        List<MateriaPlanEstudianteResponse> materias
) {
}
