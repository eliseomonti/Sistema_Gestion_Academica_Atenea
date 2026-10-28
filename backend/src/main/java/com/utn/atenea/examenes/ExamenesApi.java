package com.utn.atenea.examenes;

import java.util.Collection;
import java.util.List;

public interface ExamenesApi {

    /** Las actas de cierre vigentes de esas inscripciones a comisión. Las que no tienen acta no vienen. */
    List<ResultadoCursada> cierresVigentes(Collection<Long> inscripcionComisionIds);

    /** Las notas vigentes de esas inscripciones a final. Las que no tienen nota no vienen. */
    List<ResultadoFinal> notasFinalesVigentes(Collection<Long> inscripcionExamenFinalIds);

    /** Las mesas de examen con esos ids. Las que no existen no vienen. */
    List<ExamenFinalResumen> buscarExamenesFinales(Collection<Long> examenFinalIds);

    /** Las mesas de esos turnos, por fecha y hora. */
    List<ExamenFinalResumen> examenesFinalesDeLosTurnos(Collection<Long> periodoIds);
}
