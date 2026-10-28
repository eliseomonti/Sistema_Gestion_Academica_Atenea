package com.utn.atenea.examenes;

import java.time.LocalDate;

/**
 * El acta de cierre vigente de una inscripción a comisión. nota es la de cursada (null si no tiene);
 * fechaVencimiento, sólo si es REGULAR.
 */
public record ResultadoCursada(
        Long inscripcionComisionId,
        CondicionCursada condicion,
        Integer nota,
        LocalDate fechaCierre,
        LocalDate fechaVencimiento
) {
}
