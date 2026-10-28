package com.utn.atenea.inscripciones.model;

import com.utn.atenea.carreras.PeriodoLectivo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.utn.atenea.carreras.PeriodoLectivo.ANUAL;
import static com.utn.atenea.carreras.PeriodoLectivo.PRIMER_CUATRIMESTRE;
import static com.utn.atenea.carreras.PeriodoLectivo.SEGUNDO_CUATRIMESTRE;
import static org.assertj.core.api.Assertions.assertThat;

class PeriodoInscripcionTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 3, 1, 8, 0);
    private static final LocalDateTime CIERRE = LocalDateTime.of(2026, 3, 15, 8, 0);

    @Test
    void laDelPrimerCuatrimestreAdmiteLasDelPrimeroYLasAnuales() {
        PeriodoInscripcion periodo = deCursada(PRIMER_CUATRIMESTRE);

        assertThat(periodo.admite(2026, PRIMER_CUATRIMESTRE)).isTrue();
        assertThat(periodo.admite(2026, ANUAL)).isTrue();
        assertThat(periodo.admite(2026, SEGUNDO_CUATRIMESTRE)).isFalse();
    }

    @Test
    void laDelSegundoCuatrimestreSoloAdmiteLasDelSegundo() {
        PeriodoInscripcion periodo = deCursada(SEGUNDO_CUATRIMESTRE);

        assertThat(periodo.admite(2026, SEGUNDO_CUATRIMESTRE)).isTrue();
        assertThat(periodo.admite(2026, PRIMER_CUATRIMESTRE)).isFalse();
        assertThat(periodo.admite(2026, ANUAL)).isFalse();
    }

    @Test
    void laAnualAdmiteTodasLasDelAnio() {
        PeriodoInscripcion periodo = deCursada(ANUAL);

        assertThat(periodo.admite(2026, PRIMER_CUATRIMESTRE)).isTrue();
        assertThat(periodo.admite(2026, SEGUNDO_CUATRIMESTRE)).isTrue();
        assertThat(periodo.admite(2026, ANUAL)).isTrue();
    }

    @Test
    void noAdmiteMateriasDeOtroAnioLectivo() {
        assertThat(deCursada(ANUAL).admite(2025, ANUAL)).isFalse();
    }

    @Test
    void estaAbiertoDesdeElInicioHastaAntesDelCierre() {
        PeriodoInscripcion periodo = deCursada(ANUAL);

        assertThat(periodo.estaAbierto(INICIO.minusSeconds(1))).isFalse();
        assertThat(periodo.estaAbierto(INICIO)).isTrue();
        assertThat(periodo.estaAbierto(CIERRE.minusSeconds(1))).isTrue();
        assertThat(periodo.estaAbierto(CIERRE)).isFalse();
    }

    @Test
    void deshabilitadoNoEstaAbiertoAunqueEsteEnFecha() {
        PeriodoInscripcion periodo = deCursada(ANUAL);
        periodo.deshabilitar();

        assertThat(periodo.estaAbierto(INICIO)).isFalse();
    }

    private static PeriodoInscripcion deCursada(PeriodoLectivo periodoLectivo) {
        return PeriodoInscripcion.deCursada(1L, 2026, periodoLectivo, INICIO, CIERRE);
    }
}
