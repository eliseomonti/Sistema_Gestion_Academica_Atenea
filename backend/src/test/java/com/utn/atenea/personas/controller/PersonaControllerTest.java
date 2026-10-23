package com.utn.atenea.personas.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Búsqueda por DNI, modificación y baja contra la base real; {@code @Transactional} deshace todo al
 * terminar cada test. La baja de un alumno con inscripciones se prueba en AlumnoControllerTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonaControllerTest {

    private static final String DNI = "99000003";
    private static final String OTRO_DNI = "99000004";

    @Autowired MockMvc mvc;

    @Test
    void buscarPorDniDevuelveLaPersonaConSusLegajos() throws Exception {
        altaDocente(DNI, "persona.test@atenea.test");

        mvc.perform(get("/api/admin/personas/dni/{dni}", DNI).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.dni").value(DNI))
                .andExpect(jsonPath("$.legajoDocente").value(notNullValue()))
                .andExpect(jsonPath("$.legajoAlumno").value(nullValue()))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void dniInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/admin/personas/dni/{dni}", DNI).with(admin()))
                .andExpect(status().isNotFound());
    }

    // --- Modificación ---

    @Test
    void modificarCambiaLosDatosYElUsuarioPasaASerElDniNuevo() throws Exception {
        String alta = altaDocente(DNI, "persona.test@atenea.test");
        long personaId = JsonPath.<Number>read(alta, "$.personaId").longValue();
        long legajo = JsonPath.<Number>read(alta, "$.legajo").longValue();
        String password = JsonPath.read(alta, "$.passwordTemporal");

        modificar(personaId, "Ana María", OTRO_DNI, "nuevo.mail@atenea.test")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"))
                .andExpect(jsonPath("$.dni").value(OTRO_DNI))
                .andExpect(jsonPath("$.email").value("nuevo.mail@atenea.test"))
                .andExpect(jsonPath("$.legajoDocente").value(legajo));

        // Se loguea con el DNI nuevo y la misma contraseña; con el viejo ya no.
        login(OTRO_DNI, password).andExpect(status().isOk());
        login(DNI, password).andExpect(status().isUnauthorized());
    }

    @Test
    void modificarSinCambiarDniNiEmailNoChocaConsigoMisma() throws Exception {
        long personaId = JsonPath.<Number>read(altaDocente(DNI, "persona.test@atenea.test"), "$.personaId").longValue();

        modificar(personaId, "Ana María", DNI, "persona.test@atenea.test")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"));
    }

    @Test
    void modificarConElDniDeOtraPersonaDevuelve409() throws Exception {
        long personaId = JsonPath.<Number>read(altaDocente(DNI, "persona.test@atenea.test"), "$.personaId").longValue();
        altaDocente(OTRO_DNI, "otra.persona@atenea.test");

        modificar(personaId, "Ana", OTRO_DNI, "persona.test@atenea.test")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void modificarConDatosInvalidosDevuelve400ConLosCampos() throws Exception {
        long personaId = JsonPath.<Number>read(altaDocente(DNI, "persona.test@atenea.test"), "$.personaId").longValue();

        modificar(personaId, " ", "30.111.222", "persona.test@atenea.test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo").value(containsInAnyOrder("nombre", "dni")));
    }

    @Test
    void modificarPersonaInexistenteDevuelve404() throws Exception {
        modificar(-1L, "Ana", DNI, "persona.test@atenea.test")
                .andExpect(status().isNotFound());
    }

    // --- Baja ---

    @Test
    void darDeBajaAUnDocenteLoDesactivaConservaSuLegajoYYaNoPuedeEntrar() throws Exception {
        String alta = altaDocente(DNI, "persona.test@atenea.test");
        long personaId = JsonPath.<Number>read(alta, "$.personaId").longValue();
        long legajo = JsonPath.<Number>read(alta, "$.legajo").longValue();

        baja(personaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.legajoDocente").value(legajo));

        login(DNI, JsonPath.read(alta, "$.passwordTemporal")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/personas/dni/{dni}", DNI).with(admin()))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void darDeBajaDosVecesDevuelve409() throws Exception {
        long personaId = JsonPath.<Number>read(altaDocente(DNI, "persona.test@atenea.test"), "$.personaId").longValue();
        baja(personaId).andExpect(status().isOk());

        baja(personaId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regla de negocio no cumplida"));
    }

    @Test
    void darDeBajaAPersonaInexistenteDevuelve404() throws Exception {
        baja(-1L).andExpect(status().isNotFound());
    }

    /** Da de alta un docente y devuelve el JSON de la respuesta. */
    private String altaDocente(String dni, String email) throws Exception {
        return mvc.perform(post("/api/admin/docentes").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "apellido": "Ruiz", "dni": "%s", "telefono": "2234567890",
                                 "fechaNacimiento": "1980-01-15", "email": "%s"}
                                """.formatted(dni, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions modificar(long personaId, String nombre, String dni, String email) throws Exception {
        return mvc.perform(put("/api/admin/personas/{personaId}", personaId).with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre": "%s", "apellido": "Ruiz", "dni": "%s", "telefono": "2239876543",
                         "fechaNacimiento": "1980-01-15", "email": "%s"}
                        """.formatted(nombre, dni, email)));
    }

    private ResultActions baja(long personaId) throws Exception {
        return mvc.perform(post("/api/admin/personas/{personaId}/baja", personaId).with(admin()));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
