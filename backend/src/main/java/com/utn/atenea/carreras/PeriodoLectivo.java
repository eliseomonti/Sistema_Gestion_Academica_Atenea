package com.utn.atenea.carreras;

import java.util.EnumSet;
import java.util.Set;

/** Mitad del año lectivo en que se dicta una materia, no el cuatrimestre de la carrera. */
public enum PeriodoLectivo {
    PRIMER_CUATRIMESTRE,
    SEGUNDO_CUATRIMESTRE,
    ANUAL;

    /** Las dos cursadas comparten parte del año: un cuatrimestre se pisa consigo mismo y con ANUAL. */
    public boolean seSuperponeCon(PeriodoLectivo otro) {
        return this == otro || this == ANUAL || otro == ANUAL;
    }

    /** Los períodos que se superponen con este, para buscar choques de horario en la base. */
    public Set<PeriodoLectivo> superpuestos() {
        Set<PeriodoLectivo> superpuestos = EnumSet.noneOf(PeriodoLectivo.class);
        for (PeriodoLectivo periodo : values()) {
            if (seSuperponeCon(periodo)) {
                superpuestos.add(periodo);
            }
        }
        return superpuestos;
    }
}
