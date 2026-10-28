package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.model.CondicionExamen;

import java.time.LocalDate;

/**
 * Un final que el estudiante ya rindió (la mesa ya se tomó) con su nota vigente. condicion, nota y
 * aprobado en null: la nota todavía no está cargada.
 */
public record FinalEstudianteResponse(
        Long inscripcionExamenFinalId,
        Long examenFinalId,
        Long materiaId,
        String nombreMateria,
        LocalDate fecha,
        CondicionExamen condicion,
        Integer nota,
        Boolean aprobado
) {
}
