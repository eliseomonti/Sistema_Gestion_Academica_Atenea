package com.utn.atenea.carreras;

import org.junit.jupiter.api.Test;

import static com.utn.atenea.carreras.PeriodoLectivo.ANUAL;
import static com.utn.atenea.carreras.PeriodoLectivo.PRIMER_CUATRIMESTRE;
import static com.utn.atenea.carreras.PeriodoLectivo.SEGUNDO_CUATRIMESTRE;
import static org.assertj.core.api.Assertions.assertThat;

class PeriodoLectivoTest {

    @Test
    void losDosCuatrimestresNoSeSuperponen() {
        assertThat(PRIMER_CUATRIMESTRE.seSuperponeCon(SEGUNDO_CUATRIMESTRE)).isFalse();
        assertThat(SEGUNDO_CUATRIMESTRE.seSuperponeCon(PRIMER_CUATRIMESTRE)).isFalse();
    }

    @Test
    void unCuatrimestreSeSuperponeConsigoMismoYConAnual() {
        assertThat(PRIMER_CUATRIMESTRE.superpuestos()).containsExactlyInAnyOrder(PRIMER_CUATRIMESTRE, ANUAL);
        assertThat(SEGUNDO_CUATRIMESTRE.superpuestos()).containsExactlyInAnyOrder(SEGUNDO_CUATRIMESTRE, ANUAL);
    }

    @Test
    void anualSeSuperponeConTodos() {
        assertThat(ANUAL.superpuestos()).containsExactlyInAnyOrder(PeriodoLectivo.values());
    }
}
