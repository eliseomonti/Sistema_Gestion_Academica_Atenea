package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.model.Carrera;
import com.utn.atenea.carreras.model.ModalidadCarrera;
import com.utn.atenea.carreras.repository.CarreraRepository;
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

/** Lista contra la base real; {@code @Transactional} deshace las carreras de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarreraControllerTest {

    private static final String ACTIVA = "AAA Carrera de prueba activa";
    private static final String INACTIVA = "AAA Carrera de prueba inactiva";

    @Autowired MockMvc mvc;
    @Autowired CarreraRepository carreraRepository;

    @BeforeEach
    void crearCarreras() {
        carreraRepository.save(new Carrera(ACTIVA, "5 años", "Ingeniero", ModalidadCarrera.PRESENCIAL));
        Carrera inactiva = new Carrera(INACTIVA, "3 años", "Técnico", ModalidadCarrera.VIRTUAL);
        inactiva.darDeBaja(LocalDate.of(2020, 3, 1));
        carreraRepository.save(inactiva);
    }

    @Test
    void listaTodasLasCarrerasOrdenadasPorNombre() throws Exception {
        mvc.perform(get("/api/admin/carreras").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value(ACTIVA))
                .andExpect(jsonPath("$[0].modalidad").value("PRESENCIAL"))
                .andExpect(jsonPath("$[0].activa").value(true))
                .andExpect(jsonPath("$[1].nombre").value(INACTIVA))
                .andExpect(jsonPath("$[1].activa").value(false));
    }

    @Test
    void sinRolAdminDevuelve403() throws Exception {
        mvc.perform(get("/api/admin/carreras").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
