package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.carreras.model.Carrera;
import com.utn.atenea.carreras.model.Materia;
import com.utn.atenea.carreras.model.ModalidadCarrera;
import com.utn.atenea.carreras.model.PlanEstudio;
import com.utn.atenea.carreras.model.PlanMateria;
import com.utn.atenea.carreras.repository.CarreraRepository;
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
class PlanMateriaControllerTest {

    private static final String URL = "/api/admin/carreras/{carreraId}/planes/{planEstudioId}/materias";

    @Autowired MockMvc mvc;
    @Autowired CarreraRepository carreraRepository;
    @Autowired PlanEstudioRepository planEstudioRepository;
    @Autowired MateriaRepository materiaRepository;
    @Autowired PlanMateriaRepository planMateriaRepository;

    Carrera carrera;
    PlanEstudio plan;
    Materia programacion;

    @BeforeEach
    void crearPlanConMaterias() {
        carrera = carreraRepository.save(
                new Carrera("Carrera de prueba materias", "2 años", "Técnico", ModalidadCarrera.PRESENCIAL));
        plan = planEstudioRepository.save(new PlanEstudio(carrera, "Plan 2024", 2024));
        programacion = agregar("Programación II de prueba", "P2", 1, PeriodoLectivo.SEGUNDO_CUATRIMESTRE);
        agregar("Inglés I de prueba", null, 1, PeriodoLectivo.PRIMER_CUATRIMESTRE);
        agregar("Programación IV de prueba", "P4", 2, PeriodoLectivo.SEGUNDO_CUATRIMESTRE);
    }

    @Test
    void sinAnioCursadoListaTodasLasMateriasDelPlanOrdenadas() throws Exception {
        mvc.perform(get(URL, carrera.getId(), plan.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Inglés I de prueba"))
                .andExpect(jsonPath("$[1].nombre").value("Programación II de prueba"))
                .andExpect(jsonPath("$[2].nombre").value("Programación IV de prueba"));
    }

    @Test
    void conAnioCursadoListaSoloLasDeEseAnio() throws Exception {
        mvc.perform(get(URL, carrera.getId(), plan.getId()).param("anioCursado", "1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].materiaId").value(programacion.getId()))
                .andExpect(jsonPath("$[1].codigo").value("P2"))
                .andExpect(jsonPath("$[1].anioCursado").value(1))
                .andExpect(jsonPath("$[1].periodoLectivo").value("SEGUNDO_CUATRIMESTRE"));
    }

    @Test
    void planDeOtraCarreraDevuelve404() throws Exception {
        Carrera otra = carreraRepository.save(
                new Carrera("Otra carrera de prueba materias", "5 años", "Ingeniero", ModalidadCarrera.PRESENCIAL));

        mvc.perform(get(URL, otra.getId(), plan.getId()).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "No existe el plan con id " + plan.getId() + " en la carrera con id " + otra.getId()));
    }

    private Materia agregar(String nombre, String codigo, int anioCursado, PeriodoLectivo periodo) {
        Materia materia = materiaRepository.save(new Materia(nombre, 4, 64));
        planMateriaRepository.save(new PlanMateria(plan, materia, codigo, anioCursado, periodo));
        return materia;
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
