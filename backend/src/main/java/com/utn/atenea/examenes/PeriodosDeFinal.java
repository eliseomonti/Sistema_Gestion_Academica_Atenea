package com.utn.atenea.examenes;

import java.util.Optional;

/**
 * Puerto: lo que examenes necesita de los turnos de finales, que son períodos de inscripción. Los
 * períodos son de inscripciones, que está arriba: lo implementa PeriodosDeFinalImpl, allá.
 */
public interface PeriodosDeFinal {

    /** El período de inscripción de tipo FINAL con ese id, o vacío si no existe o es de CURSADA. */
    Optional<PeriodoDeFinal> buscar(Long periodoId);
}
