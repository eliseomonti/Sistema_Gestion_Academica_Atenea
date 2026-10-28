package com.utn.atenea.examenes.dto;

import java.time.LocalDate;
import java.util.List;

/** La planilla de una mesa: todos los anotados que no se dieron de baja, por apellido. */
public record NotasExamenFinalResponse(
        Long examenFinalId,
        Long materiaId,
        String nombreMateria,
        LocalDate fecha,
        List<NotaEstudianteResponse> notas
) {
}
