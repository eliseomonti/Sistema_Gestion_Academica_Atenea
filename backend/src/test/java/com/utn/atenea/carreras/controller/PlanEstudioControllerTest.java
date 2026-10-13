package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.model.Carrera;
import com.utn.atenea.carreras.model.ModalidadCarrera;
import com.utn.atenea.carreras.model.PlanEstudio;
import com.utn.atenea.carreras.repository.CarreraRepository;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lista contra la base real; {@code @Transactional} deshace los datos de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanEstudioControllerTest {

    @Autowired MockMvc mvc;
    @Autowired CarreraRepository carreraRepository;
    @Autowired PlanEstudioRepository planEstudioRepository;

    Carrera carrera;

    @BeforeEach
    void crearCarreraConPlanes() {
        carrera = carreraRepository.save(
                new Carrera("Carrera de prueba planes", "5 años", "Ingeniero", ModalidadCarrera.PRESENCIAL));
        PlanEstudio viejo = new PlanEstudio(carrera, "Plan 2003", 2003);
        viejo.darDeBaja(LocalDate.of(2024, 3, 1));
        planEstudioRepository.save(viejo);
        planEstudioRepository.save(new PlanEstudio(carrera, "Plan 2024", 2024));
    }

    @Test
    void listaTodosLosPlanesDeLaCarreraDelMasNuevoAlMasViejo() throws Exception {
        mvc.perform(get("/api/admin/carreras/{id}/planes", carrera.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Plan 2024"))
                .andExpect(jsonPath("$[0].vigente").value(true))
                .andExpect(jsonPath("$[1].nombre").value("Plan 2003"))
                .andExpect(jsonPath("$[1].vigente").value(false));
    }

    @Test
    void carreraSinPlanesDevuelveListaVacia() throws Exception {
        Carrera sinPlanes = carreraRepository.save(
                new Carrera("Carrera de prueba sin planes", "3 años", "Técnico", ModalidadCarrera.VIRTUAL));

        mvc.perform(get("/api/admin/carreras/{id}/planes", sinPlanes.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void carreraInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/admin/carreras/{id}/planes", -1).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la carrera con id -1"));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
