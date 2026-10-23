package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.carreras.model.Carrera;
import com.utn.atenea.carreras.model.ModalidadCarrera;
import com.utn.atenea.carreras.model.PlanEstudio;
import com.utn.atenea.carreras.repository.CarreraRepository;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Contra la base real; {@code @Transactional} deshace las carreras y planes de prueba. */
@SpringBootTest
@Transactional
class CarrerasApiImplTest {

    @Autowired CarrerasApi carrerasApi;
    @Autowired CarreraRepository carreraRepository;
    @Autowired PlanEstudioRepository planEstudioRepository;

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

    @Test
    void planVigenteDeCarreraActivaAdmiteInscripciones() {
        assertThat(carrerasApi.buscarPlan(vigente.getId()))
                .contains(new PlanEstudioResumen(vigente.getId(), activa.getId(), true));
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
}
