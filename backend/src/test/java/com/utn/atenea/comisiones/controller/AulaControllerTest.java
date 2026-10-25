package com.utn.atenea.comisiones.controller;

import com.utn.atenea.comisiones.model.Aula;
import com.utn.atenea.comisiones.repository.AulaRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lista contra la base real; {@code @Transactional} deshace las aulas de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AulaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired AulaRepository aulaRepository;

    @BeforeEach
    void crearAulas() {
        aulaRepository.save(new Aula("zz Laboratorio 5 prueba", 30));
        aulaRepository.save(new Aula("zz Aula 108 prueba", null));
        Aula cerrada = new Aula("zz Aula cerrada prueba", 20);
        cerrada.darDeBaja(LocalDate.of(2025, 3, 1));
        aulaRepository.save(cerrada);
    }

    @Test
    void listaLasAulasActivasOrdenadasPorNombre() throws Exception {
        // Las de prueba empiezan con "zz" para quedar al final aunque la base ya tenga aulas cargadas.
        mvc.perform(get("/api/admin/aulas").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[-2].nombre").value("zz Aula 108 prueba"))
                .andExpect(jsonPath("$[-2].capacidad").doesNotExist())
                .andExpect(jsonPath("$[-1].nombre").value("zz Laboratorio 5 prueba"))
                .andExpect(jsonPath("$[-1].capacidad").value(30))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("zz Aula cerrada prueba"))));
    }

    @Test
    void unDocenteNoPuedeListarAulas() throws Exception {
        mvc.perform(get("/api/admin/aulas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
