package com.utn.atenea.examenes.dto;

import com.utn.atenea.examenes.CondicionCursada;

import java.time.LocalDate;

/** El acta de cierre vigente de la cursada. fechaVencimiento, sólo si es REGULAR. */
public record CierreCursadaEstudianteResponse(
        CondicionCursada condicion,
        Integer nota,
        LocalDate fechaCierre,
        LocalDate fechaVencimiento
) {
}
