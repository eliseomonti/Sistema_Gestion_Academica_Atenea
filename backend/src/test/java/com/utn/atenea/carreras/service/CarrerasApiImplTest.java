package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.CorrelativaDelPlan;
import com.utn.atenea.carreras.EstadoCorrelativa;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.MateriaResumen;
import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.carreras.model.Carrera;
import com.utn.atenea.carreras.model.Correlatividad;
import com.utn.atenea.carreras.model.Materia;
import com.utn.atenea.carreras.model.ModalidadCarrera;
import com.utn.atenea.carreras.model.PlanEstudio;
import com.utn.atenea.carreras.model.PlanMateria;
import com.utn.atenea.carreras.repository.CarreraRepository;
import com.utn.atenea.carreras.repository.CorrelatividadRepository;
import com.utn.atenea.carreras.repository.MateriaRepository;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import com.utn.atenea.carreras.repository.PlanMateriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Contra la base real; {@code @Transactional} deshace las carreras y planes de prueba. */
@SpringBootTest
@Transactional
class CarrerasApiImplTest {

    @Autowired CarrerasApi carrerasApi;
    @Autowired CarreraRepository carreraRepository;
    @Autowired PlanEstudioRepository planEstudioRepository;
    @Autowired MateriaRepository materiaRepository;
    @Autowired PlanMateriaRepository planMateriaRepository;
    @Autowired CorrelatividadRepository correlatividadRepository;

    Carrera activa;
    PlanEstudio vigente;
    PlanEstudio discontinuado;
    PlanEstudio deCarreraInactiva;

    @BeforeEach
    void crearCarrerasYPlanes() {
        activa = carreraRepository.save(
                new Carrera("Carrera de prueba api activa", "5 años", "Ingeniero", ModalidadCarrera.PRESENCIAL));
        vigente = planEstudioRepository.save(new PlanEstudio(activa, "Plan 2024", 2024));
        discontinuado = new PlanEstudio(activa, "Plan 2003", 2003);
        discontinuado.darDeBaja(LocalDate.of(2024, 3, 1));
        planEstudioRepository.save(discontinuado);

        Carrera inactiva = new Carrera("Carrera de prueba api inactiva", "3 años", "Técnico", ModalidadCarrera.VIRTUAL);
        inactiva.darDeBaja(LocalDate.of(2020, 3, 1));
        carreraRepository.save(inactiva);
        deCarreraInactiva = planEstudioRepository.save(new PlanEstudio(inactiva, "Plan 2015", 2015));
    }

    /** Programación II (1° año, 2° cuatrimestre), Inglés I (1° año, 1° cuatrimestre) y Análisis (2° año, anual). */
    private void agregarMateriasAlPlanVigente() {
        agregar(vigente, "Programación II de prueba", 1, PeriodoLectivo.SEGUNDO_CUATRIMESTRE);
        agregar(vigente, "Inglés I de prueba", 1, PeriodoLectivo.PRIMER_CUATRIMESTRE);
        agregar(vigente, "Análisis de prueba", 2, PeriodoLectivo.ANUAL);
    }

    private Materia agregar(PlanEstudio plan, String nombre, int anioCursado, PeriodoLectivo periodo) {
        Materia materia = materiaRepository.save(new Materia(nombre, 4, 64));
        planMateriaRepository.save(new PlanMateria(plan, materia, null, anioCursado, periodo));
        return materia;
    }

    @Test
    void existeCarreraIncluyeLasDadasDeBaja() {
        assertThat(carrerasApi.existeCarrera(activa.getId())).isTrue();
        assertThat(carrerasApi.existeCarrera(deCarreraInactiva.getCarrera().getId())).isTrue();
        assertThat(carrerasApi.existeCarrera(-1L)).isFalse();
    }

    @Test
    void planVigenteDeCarreraActivaAdmiteInscripciones() {
        assertThat(carrerasApi.buscarPlan(vigente.getId()))
                .contains(new PlanEstudioResumen(vigente.getId(), activa.getId(), "Carrera de prueba api activa",
                        "Plan 2024", true));
    }

    @Test
    void planDiscontinuadoNoAdmiteInscripciones() {
        assertThat(carrerasApi.buscarPlan(discontinuado.getId()))
                .hasValueSatisfying(plan -> assertThat(plan.admiteInscripciones()).isFalse());
    }

    @Test
    void planVigenteDeCarreraInactivaNoAdmiteInscripciones() {
        assertThat(carrerasApi.buscarPlan(deCarreraInactiva.getId()))
                .hasValueSatisfying(plan -> assertThat(plan.admiteInscripciones()).isFalse());
    }

    @Test
    void planInexistenteDevuelveVacio() {
        assertThat(carrerasApi.buscarPlan(-1L)).isEmpty();
    }

    @Test
    void idsDePlanesIncluyeVigentesYDiscontinuados() {
        assertThat(carrerasApi.idsDePlanes(activa.getId()))
                .containsExactlyInAnyOrder(vigente.getId(), discontinuado.getId());
    }

    @Test
    void cantidadDeAniosEsElMayorAnioDeCursadoDeSusMaterias() {
        agregarMateriasAlPlanVigente();

        assertThat(carrerasApi.cantidadDeAnios(vigente.getId())).isEqualTo(2);
    }

    @Test
    void planSinMateriasTieneCeroAnios() {
        assertThat(carrerasApi.cantidadDeAnios(discontinuado.getId())).isZero();
    }

    @Test
    void buscarMateriaDelPlanDevuelveSuAnioYPeriodoEnEsePlan() {
        Materia programacion = agregar(vigente, "Programación II de prueba", 1, PeriodoLectivo.SEGUNDO_CUATRIMESTRE);

        assertThat(carrerasApi.buscarMateriaDelPlan(vigente.getId(), programacion.getId()))
                .contains(new MateriaDelPlan(programacion.getId(), "Programación II de prueba", 1,
                        PeriodoLectivo.SEGUNDO_CUATRIMESTRE));
    }

    @Test
    void materiaDeOtroPlanNoSeEncuentra() {
        Materia deOtroPlan = agregar(discontinuado, "Materia del plan viejo", 1, PeriodoLectivo.ANUAL);

        assertThat(carrerasApi.buscarMateriaDelPlan(vigente.getId(), deOtroPlan.getId())).isEmpty();
    }

    @Test
    void materiasDelPlanVienenOrdenadasPorAnioYPeriodo() {
        agregarMateriasAlPlanVigente();

        assertThat(carrerasApi.materiasDelPlan(vigente.getId()))
                .extracting(MateriaDelPlan::nombre)
                .containsExactly("Inglés I de prueba", "Programación II de prueba", "Análisis de prueba");
    }

    @Test
    void buscarMateriasDevuelveSuNombre() {
        Materia algebra = materiaRepository.save(new Materia("Álgebra de prueba api", 4, 64));

        assertThat(carrerasApi.buscarMaterias(List.of(algebra.getId(), -1L)))
                .containsExactly(new MateriaResumen(algebra.getId(), "Álgebra de prueba api"));
    }

    @Test
    void correlatividadesSonPorMateriaYSoloLasDelPlan() {
        PlanMateria analisis = agregarAlPlan(vigente, "Análisis de prueba");
        PlanMateria algebra = agregarAlPlan(vigente, "Álgebra de prueba");
        PlanMateria fisica = agregarAlPlan(vigente, "Física de prueba");
        correlatividadRepository.save(
                new Correlatividad(analisis, algebra, EstadoCorrelativa.CURSADA, EstadoCorrelativa.APROBADA));
        correlatividadRepository.save(new Correlatividad(fisica, analisis, null, EstadoCorrelativa.APROBADA));
        PlanMateria delPlanViejo = agregarAlPlan(discontinuado, "Análisis del plan viejo");
        correlatividadRepository.save(new Correlatividad(
                delPlanViejo, agregarAlPlan(discontinuado, "Álgebra del plan viejo"), EstadoCorrelativa.CURSADA, null));

        assertThat(carrerasApi.correlatividades(vigente.getId())).containsExactlyInAnyOrder(
                new CorrelativaDelPlan(analisis.getMateria().getId(), algebra.getMateria().getId(),
                        EstadoCorrelativa.CURSADA, EstadoCorrelativa.APROBADA),
                new CorrelativaDelPlan(fisica.getMateria().getId(), analisis.getMateria().getId(),
                        null, EstadoCorrelativa.APROBADA));
    }

    private PlanMateria agregarAlPlan(PlanEstudio plan, String nombre) {
        Materia materia = materiaRepository.save(new Materia(nombre, 4, 64));
        return planMateriaRepository.save(new PlanMateria(plan, materia, null, 1, PeriodoLectivo.ANUAL));
    }
}
