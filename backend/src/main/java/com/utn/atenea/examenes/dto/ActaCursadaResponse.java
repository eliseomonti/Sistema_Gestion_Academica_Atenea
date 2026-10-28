package com.utn.atenea.examenes.dto;

import java.util.List;

/** El acta de cierre de la materia de la comisión: todos los anotados que no se dieron de baja, por apellido. */
public record ActaCursadaResponse(
        Long comisionMateriaId,
        List<CierreEstudianteResponse> estudiantes
) {
}
