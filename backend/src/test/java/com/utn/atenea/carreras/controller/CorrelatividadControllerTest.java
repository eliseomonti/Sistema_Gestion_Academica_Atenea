package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.EstadoCorrelativa;
import com.utn.atenea.carreras.PeriodoLectivo;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lista contra la base real; {@code @Transactional} deshace los datos de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CorrelatividadControllerTest {

    private static final String URL = "/api/admin/carreras/{carreraId}/planes/{planEstudioId}/correlatividades";

    @Autowired MockMvc mvc;
    @Autowired CarreraRepository carreraRepository;
    @Autowired PlanEstudioRepository planEstudioRepository;
    @Autowired MateriaRepository materiaRepository;
    @Autowired PlanMateriaRepository planMateriaRepository;
    @Autowired CorrelatividadRepository correlatividadRepository;

    Carrera carrera;
    PlanEstudio plan;

    @BeforeEach
    void crearPlanConCorrelatividades() {
        carrera = carreraRepository.save(
                new Carrera("Carrera de prueba correlatividades", "2 años", "Técnico", ModalidadCarrera.PRESENCIAL));
        plan = planEstudioRepository.save(new PlanEstudio(carrera, "Plan 2024", 2024));
        PlanMateria programacionI = agregar("Programación I de prueba", "01", 1);
        PlanMateria matematica = agregar("Matemática de prueba", "02", 1);
        PlanMateria programacionII = agregar("Programación II de prueba", "03", 2);
        correlatividadRepository.save(new Correlatividad(
                programacionII, programacionI, EstadoCorrelativa.CURSADA, EstadoCorrelativa.APROBADA));
        correlatividadRepository.save(new Correlatividad(
                programacionII, matematica, EstadoCorrelativa.APROBADA, null));
    }

    @Test
    void listaTodasLasMateriasDelPlanConLosCodigosDeSusCorrelativas() throws Exception {
        mvc.perform(get(URL, carrera.getId(), plan.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Programación I de prueba"))
                .andExpect(jsonPath("$[0].cursadasParaCursar.length()").value(0))
                .andExpect(jsonPath("$[2].codigo").value("03"))
                .andExpect(jsonPath("$[2].anioCursado").value(2))
                .andExpect(jsonPath("$[2].periodoLectivo").value("ANUAL"))
                .andExpect(jsonPath("$[2].cursadasParaCursar[0]").value("01"))
                .andExpect(jsonPath("$[2].aprobadasParaCursar[0]").value("02"))
                .andExpect(jsonPath("$[2].cursadasParaRendir.length()").value(0))
                .andExpect(jsonPath("$[2].aprobadasParaRendir[0]").value("01"));
    }

    @Test
    void planDeOtraCarreraDevuelve404() throws Exception {
        Carrera otra = carreraRepository.save(new Carrera(
                "Otra carrera de prueba correlatividades", "5 años", "Ingeniero", ModalidadCarrera.PRESENCIAL));

        mvc.perform(get(URL, otra.getId(), plan.getId()).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unEstudianteNoPuedeEntrar() throws Exception {
        mvc.perform(get(URL, carrera.getId(), plan.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
                .andExpect(status().isForbidden());
    }

    private PlanMateria agregar(String nombre, String codigo, int anioCursado) {
        Materia materia = materiaRepository.save(new Materia(nombre, 4, 64));
        return planMateriaRepository.save(new PlanMateria(plan, materia, codigo, anioCursado, PeriodoLectivo.ANUAL));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
