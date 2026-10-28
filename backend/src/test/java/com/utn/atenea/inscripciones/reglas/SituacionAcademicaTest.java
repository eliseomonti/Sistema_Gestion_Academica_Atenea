package com.utn.atenea.inscripciones.reglas;

import com.utn.atenea.carreras.CorrelativaDelPlan;
import com.utn.atenea.carreras.EstadoCorrelativa;
import com.utn.atenea.examenes.CondicionCursada;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica.Cursada;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica.FinalRendido;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SituacionAcademicaTest {

    static final LocalDate HOY = LocalDate.of(2026, 7, 10);
    static final Long ALGEBRA = 1L;
    static final Long ANALISIS = 2L;
    static final Long FISICA = 3L;

    @Test
    void sinNadaLaMateriaNoEstaCursada() {
        assertThat(situacion(List.of(), List.of()).estado(ALGEBRA)).isEqualTo(EstadoMateria.NO_CURSADA);
    }

    @Test
    void sinActaDeCierreEstaCursando() {
        assertThat(situacion(List.of(cursando(ALGEBRA)), List.of()).estado(ALGEBRA))
                .isEqualTo(EstadoMateria.CURSANDO);
    }

    @Test
    void laRegularidadValeHastaElDiaDelVencimientoInclusive() {
        assertThat(situacion(List.of(regular(ALGEBRA, HOY)), List.of()).estado(ALGEBRA))
                .isEqualTo(EstadoMateria.REGULAR);
        assertThat(situacion(List.of(regular(ALGEBRA, HOY.minusDays(1))), List.of()).estado(ALGEBRA))
                .isEqualTo(EstadoMateria.REGULARIDAD_VENCIDA);
    }

    @Test
    void laPromocionApruebaLaMateria() {
        Cursada promocionada = new Cursada(ALGEBRA, CondicionCursada.PROMOCIONADO, 9, null);

        assertThat(situacion(List.of(promocionada), List.of()).estado(ALGEBRA)).isEqualTo(EstadoMateria.APROBADA);
    }

    @Test
    void conElFinalAprobadoLaMateriaEstaAprobada() {
        assertThat(situacion(List.of(regular(ALGEBRA, HOY.plusYears(1))), List.of(new FinalRendido(ALGEBRA, 7, true)))
                .estado(ALGEBRA)).isEqualTo(EstadoMateria.APROBADA);
    }

    @Test
    void conElFinalDesaprobadoSigueRegular() {
        assertThat(situacion(List.of(regular(ALGEBRA, HOY.plusYears(1))), List.of(new FinalRendido(ALGEBRA, 4, false)))
                .estado(ALGEBRA)).isEqualTo(EstadoMateria.REGULAR);
    }

    @Test
    void siLaCursoVariasVecesValeLaMasAvanzada() {
        Cursada noRegular = new Cursada(ALGEBRA, CondicionCursada.NO_REGULAR, 2, null);

        assertThat(situacion(List.of(noRegular, regular(ALGEBRA, HOY.plusYears(1))), List.of()).estado(ALGEBRA))
                .isEqualTo(EstadoMateria.REGULAR);
        assertThat(situacion(List.of(regular(ALGEBRA, HOY.minusDays(1)), cursando(ALGEBRA)), List.of())
                .estado(ALGEBRA)).isEqualTo(EstadoMateria.CURSANDO);
    }

    @Test
    void correlativaCursadaSeCumpleConRegularidadVigenteOConLaMateriaAprobada() {
        List<CorrelativaDelPlan> analisisPideAlgebraCursada = List.of(paraCursar(ANALISIS, ALGEBRA, EstadoCorrelativa.CURSADA));

        assertThat(new SituacionAcademica(analisisPideAlgebraCursada, List.of(regular(ALGEBRA, HOY)), List.of(), HOY)
                .puedeCursar(ANALISIS)).isTrue();
        assertThat(new SituacionAcademica(analisisPideAlgebraCursada, List.of(), List.of(new FinalRendido(ALGEBRA, 7, true)), HOY)
                .puedeCursar(ANALISIS)).isTrue();
        assertThat(new SituacionAcademica(analisisPideAlgebraCursada, List.of(regular(ALGEBRA, HOY.minusDays(1))), List.of(), HOY)
                .faltanParaCursar(ANALISIS)).containsExactly(ALGEBRA);
        assertThat(new SituacionAcademica(analisisPideAlgebraCursada, List.of(cursando(ALGEBRA)), List.of(), HOY)
                .puedeCursar(ANALISIS)).isFalse();
    }

    @Test
    void correlativaAprobadaNoSeCumpleConLaRegularidad() {
        List<CorrelativaDelPlan> analisisPideAlgebraAprobada = List.of(paraCursar(ANALISIS, ALGEBRA, EstadoCorrelativa.APROBADA));

        assertThat(new SituacionAcademica(analisisPideAlgebraAprobada, List.of(regular(ALGEBRA, HOY)), List.of(), HOY)
                .faltanParaCursar(ANALISIS)).containsExactly(ALGEBRA);
    }

    @Test
    void faltanParaCursarDevuelveTodasLasQueNoCumple() {
        List<CorrelativaDelPlan> fisicaPideAmbas = List.of(
                paraCursar(FISICA, ALGEBRA, EstadoCorrelativa.CURSADA),
                paraCursar(FISICA, ANALISIS, EstadoCorrelativa.CURSADA));

        assertThat(new SituacionAcademica(fisicaPideAmbas, List.of(regular(ALGEBRA, HOY)), List.of(), HOY)
                .faltanParaCursar(FISICA)).containsExactly(ANALISIS);
    }

    @Test
    void unaCorrelativaSoloParaRendirNoImpideCursar() {
        List<CorrelativaDelPlan> soloParaRendir = List.of(
                new CorrelativaDelPlan(ANALISIS, ALGEBRA, null, EstadoCorrelativa.APROBADA));
        SituacionAcademica situacion = new SituacionAcademica(
                soloParaRendir, List.of(regular(ANALISIS, HOY)), List.of(), HOY);

        assertThat(situacion.faltanParaCursar(ANALISIS)).isEmpty();
        assertThat(situacion.puedeRendir(ANALISIS, HOY)).isFalse();
        assertThat(situacion.faltanParaRendir(ANALISIS)).containsExactly(ALGEBRA);
    }

    @Test
    void noPuedeCursarLoQueYaTieneRegularOAprobado() {
        assertThat(situacion(List.of(regular(ALGEBRA, HOY)), List.of()).puedeCursar(ALGEBRA)).isFalse();
        assertThat(situacion(List.of(), List.of(new FinalRendido(ALGEBRA, 7, true))).puedeCursar(ALGEBRA)).isFalse();
        assertThat(situacion(List.of(regular(ALGEBRA, HOY.minusDays(1))), List.of()).puedeCursar(ALGEBRA)).isTrue();
    }

    @Test
    void soloRindeLoQueTieneRegular() {
        assertThat(situacion(List.of(regular(ALGEBRA, HOY)), List.of()).puedeRendir(ALGEBRA, HOY)).isTrue();
        assertThat(situacion(List.of(cursando(ALGEBRA)), List.of()).puedeRendir(ALGEBRA, HOY)).isFalse();
        assertThat(situacion(List.of(regular(ALGEBRA, HOY.minusDays(1))), List.of()).puedeRendir(ALGEBRA, HOY)).isFalse();
        assertThat(situacion(List.of(), List.of(new FinalRendido(ALGEBRA, 7, true))).puedeRendir(ALGEBRA, HOY)).isFalse();
    }

    @Test
    void laRegularidadTieneQueValerElDiaDelExamen() {
        SituacionAcademica situacion = situacion(List.of(regular(ALGEBRA, HOY.plusDays(10))), List.of());

        assertThat(situacion.puedeRendir(ALGEBRA, HOY.plusDays(10))).isTrue();
        assertThat(situacion.puedeRendir(ALGEBRA, HOY.plusDays(11))).isFalse();
    }

    @Test
    void laNotaDeAprobacionEsLaDelFinalOLaDeLaPromocion() {
        Cursada promocionada = new Cursada(ALGEBRA, CondicionCursada.PROMOCIONADO, 9, null);

        assertThat(situacion(List.of(promocionada), List.of()).notaDeAprobacion(ALGEBRA)).contains(9);
        assertThat(situacion(List.of(regular(ALGEBRA, HOY)), List.of(new FinalRendido(ALGEBRA, 7, true)))
                .notaDeAprobacion(ALGEBRA)).contains(7);
        assertThat(situacion(List.of(regular(ALGEBRA, HOY)), List.of(new FinalRendido(ALGEBRA, 4, false)))
                .notaDeAprobacion(ALGEBRA)).isEmpty();
    }

    @Test
    void elVencimientoEsElDeLaRegularidadMasLarga() {
        SituacionAcademica situacion = situacion(
                List.of(regular(ALGEBRA, HOY.plusYears(1)), regular(ALGEBRA, HOY.plusYears(2))), List.of());

        assertThat(situacion.vencimientoDeLaRegularidad(ALGEBRA)).contains(HOY.plusYears(2));
        assertThat(situacion(List.of(regular(ALGEBRA, HOY.minusDays(1))), List.of())
                .vencimientoDeLaRegularidad(ALGEBRA)).isEmpty();
    }

    private static SituacionAcademica situacion(List<Cursada> cursadas, List<FinalRendido> finales) {
        return new SituacionAcademica(List.of(), cursadas, finales, HOY);
    }

    private static Cursada cursando(Long materiaId) {
        return new Cursada(materiaId, null, null, null);
    }

    private static Cursada regular(Long materiaId, LocalDate vencimiento) {
        return new Cursada(materiaId, CondicionCursada.REGULAR, 6, vencimiento);
    }

    private static CorrelativaDelPlan paraCursar(Long materiaId, Long correlativaId, EstadoCorrelativa estado) {
        return new CorrelativaDelPlan(materiaId, correlativaId, estado, null);
    }
}
