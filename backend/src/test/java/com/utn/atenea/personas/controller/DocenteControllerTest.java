package com.utn.atenea.personas.controller;

import com.jayway.jsonpath.JsonPath;
import com.utn.atenea.personas.dto.DatosPersonalesRequest;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.PersonaRepository;
import com.utn.atenea.personas.service.PersonaService;
import com.utn.atenea.seguridad.CredencialesIniciales;
import com.utn.atenea.seguridad.NombreRol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alta, asignación del rol y listado de docentes contra la base real; {@code @Transactional} deshace todo al terminar cada test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocenteControllerTest {

    private static final String DNI = "99000001";

    @Autowired MockMvc mvc;
    @Autowired PersonaRepository personaRepository;
    @Autowired PersonaService personaService;
    @Autowired JwtDecoder jwtDecoder;

    @Test
    void altaCreaElDocenteConLegajoYUnUsuarioQuePuedeLoguearse() throws Exception {
        long legajoEsperado = personaRepository.ultimoLegajoDocente() + 1;

        String respuesta = alta(DNI, "docente.test@atenea.test", admin())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(DNI))
                .andReturn().getResponse().getContentAsString();

        long personaId = JsonPath.<Number>read(respuesta, "$.personaId").longValue();
        long legajo = JsonPath.<Number>read(respuesta, "$.legajo").longValue();
        String passwordTemporal = JsonPath.read(respuesta, "$.passwordTemporal");

        assertThat(legajo).isEqualTo(legajoEsperado);
        Persona docente = personaRepository.findById(personaId).orElseThrow();
        assertThat(docente.getLegajoDocente()).isEqualTo(legajoEsperado);
        assertThat(docente.esEstudiante()).isFalse();
        assertThat(docente.getNombre()).isEqualTo("María José");

        // Las credenciales devueltas sirven para loguearse y el token trae el rol de docente.
        assertThat(roles(DNI, passwordTemporal)).containsExactly("ROLE_DOCENTE");
    }

    @Test
    void dniRepetidoDevuelve409() throws Exception {
        alta(DNI, "docente.test@atenea.test", admin()).andExpect(status().isCreated());

        alta(DNI, "otro.mail@atenea.test", admin())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void datosInvalidosDevuelve400ConLosCampos() throws Exception {
        mvc.perform(post("/api/admin/docentes").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": " ", "apellido": "Pérez", "dni": "30.111.222",
                                 "telefono": "2234567890", "fechaNacimiento": "1985-03-20",
                                 "email": "docente.test@atenea.test"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo").value(containsInAnyOrder("nombre", "dni")));
    }

    @Test
    void sinRolAdminDevuelve403() throws Exception {
        alta(DNI, "docente.test@atenea.test", jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unEstudianteExistentePasaASerTambienDocente() throws Exception {
        // Se arma un estudiante sin pasar por su endpoint, que necesita una carrera y un plan.
        Persona estudiante = personaService.crear(new DatosPersonalesRequest(
                "Lucía", "Gómez", DNI, "2234567890", LocalDate.of(2000, 5, 10), "estudiante.test@atenea.test"));
        personaService.asignarLegajoEstudiante(estudiante);
        CredencialesIniciales credenciales = personaService.crearUsuario(estudiante, NombreRol.ROLE_ESTUDIANTE);
        long legajoEsperado = personaRepository.ultimoLegajoDocente() + 1;

        mvc.perform(put("/api/admin/docentes/{personaId}", estudiante.getId()).with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legajoDocente").value(legajoEsperado))
                .andExpect(jsonPath("$.legajoEstudiante").value(estudiante.getLegajoEstudiante()));

        // Conserva su contraseña y el token trae los dos roles.
        assertThat(roles(credenciales.username(), credenciales.passwordTemporal()))
                .containsExactlyInAnyOrder("ROLE_ESTUDIANTE", "ROLE_DOCENTE");
    }

    @Test
    void asignarRolAQuienYaEsDocenteDevuelve409() throws Exception {
        String respuesta = alta(DNI, "docente.test@atenea.test", admin())
                .andReturn().getResponse().getContentAsString();
        long personaId = JsonPath.<Number>read(respuesta, "$.personaId").longValue();

        mvc.perform(put("/api/admin/docentes/{personaId}", personaId).with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void asignarRolAPersonaInexistenteDevuelve404() throws Exception {
        mvc.perform(put("/api/admin/docentes/{personaId}", -1L).with(admin()))
                .andExpect(status().isNotFound());
    }

    // --- Listado ---
    // Los DNI de prueba empiezan con 990000 y se buscan por ese prefijo, para que los tests no
    // dependan de los docentes que ya haya en la base.

    @Test
    void listaSoloDocentesOrdenadosPorApellido() throws Exception {
        Persona ruiz = docente("99000031", "Ana", "Ruiz");
        docente("99000032", "Pedro", "Acosta");
        personaService.asignarLegajoEstudiante(persona("99000033", "Lucía", "Gómez"));

        listar("buscar", "990000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Acosta", "Ruiz")))
                .andExpect(jsonPath("$.content[1].nombre").value("Ana"))
                .andExpect(jsonPath("$.content[1].dni").value("99000031"))
                .andExpect(jsonPath("$.content[1].email").value("99000031@atenea.test"))
                .andExpect(jsonPath("$.content[1].legajo").value(ruiz.getLegajoDocente()))
                .andExpect(jsonPath("$.content[1].personaId").value(ruiz.getId()))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void buscaPorNombreCompletoYPorLegajoDeDocente() throws Exception {
        Persona ruiz = docente("99000031", "Ana", "Ruiz");
        docente("99000032", "Pedro", "Acosta");

        listar("buscar", "ana RUIZ 990000")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Ruiz")));
        // El legajo también puede ser el comienzo del DNI de otro docente de la base: se pide la
        // página más grande y se controla que esté Ruiz y no Acosta.
        listar("buscar", ruiz.getLegajoDocente().toString(), "size", "100")
                .andExpect(jsonPath("$.content[*].apellido").value(hasItem("Ruiz")))
                .andExpect(jsonPath("$.content[*].apellido").value(not(hasItem("Acosta"))));
    }

    @Test
    void porDefectoListaLosActivosYConActivoFalseLosDadosDeBaja() throws Exception {
        docente("99000031", "Ana", "Ruiz");
        Persona acosta = docente("99000032", "Pedro", "Acosta");
        personaService.darDeBaja(acosta.getId());

        listar("buscar", "990000")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Ruiz")));
        listar("buscar", "990000", "activo", "false")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Acosta")));
    }

    private Persona docente(String dni, String nombre, String apellido) {
        Persona docente = persona(dni, nombre, apellido);
        personaService.asignarLegajoDocente(docente);
        return docente;
    }

    private Persona persona(String dni, String nombre, String apellido) {
        return personaService.crear(new DatosPersonalesRequest(
                nombre, apellido, dni, "2234567890", LocalDate.of(1980, 1, 15), dni + "@atenea.test"));
    }

    /** GET del listado con los parámetros dados de a pares: nombre, valor, nombre, valor... */
    private ResultActions listar(String... parametros) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/admin/docentes").with(admin());
        for (int i = 0; i < parametros.length; i += 2) {
            request.param(parametros[i], parametros[i + 1]);
        }
        return mvc.perform(request);
    }

    private List<String> roles(String username, String password) throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jwtDecoder.decode(JsonPath.read(login, "$.accessToken")).getClaimAsStringList("roles");
    }

    private ResultActions alta(String dni, String email, RequestPostProcessor usuario) throws Exception {
        return mvc.perform(post("/api/admin/docentes").with(usuario).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre": "  María  José ", "apellido": "Pérez", "dni": "%s",
                         "telefono": "2234567890", "fechaNacimiento": "1985-03-20", "email": "%s"}
                        """.formatted(dni, email)));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
