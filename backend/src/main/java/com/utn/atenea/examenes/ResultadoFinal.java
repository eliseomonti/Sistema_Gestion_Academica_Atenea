package com.utn.atenea.examenes;

/** La nota vigente de una inscripción a final. nota en null: estuvo ausente. */
public record ResultadoFinal(
        Long inscripcionExamenFinalId,
        Integer nota,
        boolean aprobado
) {
}
