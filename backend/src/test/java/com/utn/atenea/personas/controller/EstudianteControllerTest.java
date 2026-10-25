package com.utn.atenea.personas.controller;

import com.jayway.jsonpath.JsonPath;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alta, inscripción, listado y baja de estudiantes contra la base real; {@code @Transactional} deshace todo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EstudianteControllerTest {

    private static final String DNI = "99000002";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PersonaRepository personaRepository;
    @Autowired JwtDecoder jwtDecoder;

    Long carreraId;
    Long planVigente;
    Long otroPlanVigenteDeLaMismaCarrera;
    Long planDiscontinuado;
    Long planDeOtraCarrera;

    @BeforeEach
    void crearCarrerasConPlanes() {
        // carrera y plan_estudio son del módulo carreras: se insertan con SQL para no depender de su código.
        carreraId = insertarCarrera("Carrera de prueba estudiantes");
        planVigente = insertarPlan(carreraId, "Plan 2024");
        otroPlanVigenteDeLaMismaCarrera = insertarPlan(carreraId, "Plan 2025");
        jdbc.update("""
                INSERT INTO plan_estudio (carrera_id, nombre, anio_inicio, vigente, fecha_baja)
                VALUES (?, 'Plan 2003', 2003, FALSE, '2024-03-01')
                """, carreraId);
        planDiscontinuado = ultimoId();

        planDeOtraCarrera = insertarPlan(insertarCarrera("Otra carrera de prueba estudiantes"), "Plan 2024");
    }

    // --- Alta de una persona nueva como estudiante ---

    @Test
    void altaCreaElEstudianteConLegajoUsuarioEInscripcion() throws Exception {
        long legajoEsperado = personaRepository.ultimoLegajoEstudiante() + 1;

        String respuesta = alta(DNI, planVigente)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(DNI))
                .andReturn().getResponse().getContentAsString();

        long personaId = JsonPath.<Number>read(respuesta, "$.personaId").longValue();
        assertThat(JsonPath.<Number>read(respuesta, "$.legajo").longValue()).isEqualTo(legajoEsperado);
        Persona estudiante = personaRepository.findById(personaId).orElseThrow();
        assertThat(estudiante.getLegajoEstudiante()).isEqualTo(legajoEsperado);
        assertThat(estudiante.esDocente()).isFalse();

        Map<String, Object> inscripcion = jdbc.queryForMap(
                "SELECT plan_estudio_id, estado, fecha_inscripcion FROM inscripcion_carrera WHERE estudiante_id = ?",
                personaId);
        assertThat(((Number) inscripcion.get("plan_estudio_id")).longValue()).isEqualTo(planVigente);
        assertThat(inscripcion.get("estado")).isEqualTo("ACTIVA");
        assertThat(((Date) inscripcion.get("fecha_inscripcion")).toLocalDate()).isEqualTo(LocalDate.now());

        // Las credenciales devueltas sirven para loguearse y el token trae el rol de estudiante.
        assertThat(roles(DNI, JsonPath.read(respuesta, "$.passwordTemporal"))).containsExactly("ROLE_ESTUDIANTE");
    }

    @Test
    void planInexistenteDevuelve404YNoCreaLaPersona() throws Exception {
        alta(DNI, -1L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));

        assertThat(personaRepository.existsByDni(DNI)).isFalse();
    }

    @Test
    void planDiscontinuadoDevuelve409YNoCreaLaPersona() throws Exception {
        alta(DNI, planDiscontinuado)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regla de negocio no cumplida"));

        assertThat(personaRepository.existsByDni(DNI)).isFalse();
    }

    @Test
    void datosInvalidosDevuelve400ConLosCampos() throws Exception {
        mvc.perform(post("/api/admin/estudiantes").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datosPersonales": {"nombre": "Lucía", "apellido": "Gómez", "dni": "07123456",
                                  "telefono": "2234567890", "fechaNacimiento": "2004-08-15",
                                  "email": "estudiante.test@atenea.test"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo")
                        .value(containsInAnyOrder("datosPersonales.dni", "planEstudioId")));
    }

    // --- Inscripción de una persona que ya existe ---

    @Test
    void unDocenteQueSeInscribePasaASerTambienEstudiante() throws Exception {
        String respuesta = mvc.perform(post("/api/admin/docentes").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "apellido": "Ruiz", "dni": "%s", "telefono": "2234567890",
                                 "fechaNacimiento": "1980-01-15", "email": "docente.test@atenea.test"}
                                """.formatted(DNI)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long personaId = JsonPath.<Number>read(respuesta, "$.personaId").longValue();
        long legajoEsperado = personaRepository.ultimoLegajoEstudiante() + 1;

        inscribir(personaId, planVigente)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personaId").value(personaId))
                .andExpect(jsonPath("$.legajoEstudiante").value(legajoEsperado))
                .andExpect(jsonPath("$.planEstudioId").value(planVigente))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        // Conserva su contraseña y el token trae los dos roles.
        assertThat(roles(DNI, JsonPath.read(respuesta, "$.passwordTemporal")))
                .containsExactlyInAnyOrder("ROLE_DOCENTE", "ROLE_ESTUDIANTE");
    }

    @Test
    void unEstudianteSeInscribeEnOtraCarreraYConservaSuLegajo() throws Exception {
        long personaId = altaEstudiante(planVigente);
        Long legajo = personaRepository.findById(personaId).orElseThrow().getLegajoEstudiante();

        inscribir(personaId, planDeOtraCarrera)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legajoEstudiante").value(legajo))
                .andExpect(jsonPath("$.planEstudioId").value(planDeOtraCarrera));
    }

    @Test
    void inscribirseDosVecesAlMismoPlanDevuelve409() throws Exception {
        long personaId = altaEstudiante(planVigente);

        inscribir(personaId, planVigente)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void conUnaInscripcionActivaNoPuedeInscribirseEnOtroPlanDeLaMismaCarrera() throws Exception {
        long personaId = altaEstudiante(planVigente);

        inscribir(personaId, otroPlanVigenteDeLaMismaCarrera)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regla de negocio no cumplida"));
    }

    @Test
    void inscribirAPersonaInexistenteDevuelve404() throws Exception {
        inscribir(-1L, planVigente).andExpect(status().isNotFound());
    }

    // --- Listado ---

    @Test
    void listaLosEstudiantesActivosDeLaCarreraOrdenadosPorApellido() throws Exception {
        altaEstudiante("99000011", "Lucía", "Gómez", planVigente);
        altaEstudiante("99000012", "Ana", "Álvarez", planVigente);
        altaEstudiante("99000013", "Juan", "Pérez", otroPlanVigenteDeLaMismaCarrera);
        altaEstudiante("99000014", "Carla", "Benítez", planDeOtraCarrera);

        listar("carreraId", carreraId.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Álvarez", "Gómez", "Pérez")))
                .andExpect(jsonPath("$.content[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.content[0].dni").value("99000012"))
                .andExpect(jsonPath("$.content[0].email").value("99000012@atenea.test"))
                .andExpect(jsonPath("$.content[0].legajo").isNumber())
                .andExpect(jsonPath("$.content[0].personaId").isNumber())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void porDefectoTraeLasInscripcionesActivasYSePuedeFiltrarPorOtroEstado() throws Exception {
        altaEstudiante("99000011", "Lucía", "Gómez", planVigente);
        long deBaja = altaEstudiante("99000012", "Ana", "Álvarez", planVigente);
        jdbc.update("UPDATE inscripcion_carrera SET estado = 'BAJA', fecha_estado = CURRENT_DATE WHERE estudiante_id = ?",
                deBaja);

        listar("carreraId", carreraId.toString())
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Gómez")));
        listar("carreraId", carreraId.toString(), "estado", "BAJA")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Álvarez")));
    }

    @Test
    void buscaPorApellidoSinTildePorNombreCompletoPorDniYPorLegajo() throws Exception {
        long gomez = altaEstudiante("99000011", "Lucía", "Gómez", planVigente);
        altaEstudiante("99000022", "Ana", "Álvarez", planVigente);
        String legajo = personaRepository.findById(gomez).orElseThrow().getLegajoEstudiante().toString();

        listar("carreraId", carreraId.toString(), "buscar", "gomez")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Gómez")));
        listar("carreraId", carreraId.toString(), "buscar", "  lucia   GOMEZ ")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Gómez")));
        listar("carreraId", carreraId.toString(), "buscar", "990000")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Álvarez", "Gómez")));
        listar("carreraId", carreraId.toString(), "buscar", "99000022")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Álvarez")));
        listar("carreraId", carreraId.toString(), "buscar", legajo)
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Gómez")));
    }

    @Test
    void losComodinesDelLikeSeBuscanComoTexto() throws Exception {
        altaEstudiante("99000011", "Lucía", "Gómez", planVigente);

        listar("carreraId", carreraId.toString(), "buscar", "%")
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void paginaLosResultados() throws Exception {
        altaEstudiante("99000011", "Lucía", "Gómez", planVigente);
        altaEstudiante("99000012", "Ana", "Álvarez", planVigente);
        altaEstudiante("99000013", "Juan", "Pérez", planVigente);

        listar("carreraId", carreraId.toString(), "size", "2")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Álvarez", "Gómez")))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
        listar("carreraId", carreraId.toString(), "size", "2", "page", "1")
                .andExpect(jsonPath("$.content[*].apellido").value(contains("Pérez")));
    }

    @Test
    void elTamanioDePaginaTieneUnMaximoDe100() throws Exception {
        listar("carreraId", carreraId.toString(), "size", "1000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void carreraInexistenteDevuelveListaVacia() throws Exception {
        listar("carreraId", "-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void estadoInvalidoDevuelve400() throws Exception {
        listar("estado", "CUALQUIERA").andExpect(status().isBadRequest());
    }

    // --- Baja ---

    @Test
    void bajaDeUnaInscripcionLaSacaDelListadoYConservaLasOtras() throws Exception {
        long personaId = altaEstudiante(planVigente);
        inscribir(personaId, planDeOtraCarrera).andExpect(status().isCreated());

        bajaDeInscripcion(personaId, inscripcionId(personaId, planVigente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BAJA"))
                .andExpect(jsonPath("$.planEstudioId").value(planVigente));

        listar("carreraId", carreraId.toString())
                .andExpect(jsonPath("$.page.totalElements").value(0));
        listar("carreraId", carreraId.toString(), "estado", "BAJA")
                .andExpect(jsonPath("$.content[*].dni").value(contains(DNI)));
        // Sigue activo en la otra carrera.
        listar("buscar", DNI)
                .andExpect(jsonPath("$.content[*].dni").value(contains(DNI)));
    }

    @Test
    void bajaDeUnaInscripcionQueNoEstaActivaDevuelve409() throws Exception {
        long personaId = altaEstudiante(planVigente);
        long inscripcionId = inscripcionId(personaId, planVigente);
        bajaDeInscripcion(personaId, inscripcionId).andExpect(status().isOk());

        bajaDeInscripcion(personaId, inscripcionId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regla de negocio no cumplida"));
    }

    @Test
    void bajaDeLaInscripcionDeOtraPersonaDevuelve404() throws Exception {
        long gomez = altaEstudiante("99000011", "Lucía", "Gómez", planVigente);
        long alvarez = altaEstudiante("99000012", "Ana", "Álvarez", planVigente);

        bajaDeInscripcion(alvarez, inscripcionId(gomez, planVigente))
                .andExpect(status().isNotFound());
    }

    // El endpoint es de personas, pero el test va acá porque necesita los planes de prueba.
    @Test
    void darDeBajaALaPersonaDaDeBajaSusInscripcionesYYaNoPuedeEntrar() throws Exception {
        String respuesta = alta(DNI, planVigente).andReturn().getResponse().getContentAsString();
        long personaId = JsonPath.<Number>read(respuesta, "$.personaId").longValue();

        mvc.perform(post("/api/admin/personas/{personaId}/baja", personaId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        listar("carreraId", carreraId.toString())
                .andExpect(jsonPath("$.page.totalElements").value(0));
        listar("carreraId", carreraId.toString(), "estado", "BAJA")
                .andExpect(jsonPath("$.content[*].dni").value(contains(DNI)));
        login(DNI, JsonPath.read(respuesta, "$.passwordTemporal"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions alta(String dni, Long planEstudioId) throws Exception {
        return alta(dni, "Lucía", "Gómez", planEstudioId);
    }

    private ResultActions alta(String dni, String nombre, String apellido, Long planEstudioId) throws Exception {
        return mvc.perform(post("/api/admin/estudiantes").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"datosPersonales": {"nombre": "%s", "apellido": "%s", "dni": "%s",
                          "telefono": "2234567890", "fechaNacimiento": "2004-08-15",
                          "email": "%s@atenea.test"},
                         "planEstudioId": %d}
                        """.formatted(nombre, apellido, dni, dni, planEstudioId)));
    }

    private long altaEstudiante(Long planEstudioId) throws Exception {
        return altaEstudiante(DNI, "Lucía", "Gómez", planEstudioId);
    }

    private long altaEstudiante(String dni, String nombre, String apellido, Long planEstudioId) throws Exception {
        String respuesta = alta(dni, nombre, apellido, planEstudioId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(respuesta, "$.personaId").longValue();
    }

    /** GET del listado con los parámetros dados de a pares: nombre, valor, nombre, valor... */
    private ResultActions listar(String... parametros) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/admin/estudiantes").with(admin());
        for (int i = 0; i < parametros.length; i += 2) {
            request.param(parametros[i], parametros[i + 1]);
        }
        return mvc.perform(request);
    }

    private ResultActions bajaDeInscripcion(long personaId, long inscripcionId) throws Exception {
        return mvc.perform(post("/api/admin/estudiantes/{personaId}/inscripciones/{inscripcionId}/baja",
                personaId, inscripcionId).with(admin()));
    }

    private long inscripcionId(long personaId, Long planEstudioId) {
        return jdbc.queryForObject("SELECT id FROM inscripcion_carrera WHERE estudiante_id = ? AND plan_estudio_id = ?",
                Long.class, personaId, planEstudioId);
    }

    private ResultActions inscribir(long personaId, Long planEstudioId) throws Exception {
        return mvc.perform(post("/api/admin/estudiantes/{personaId}/inscripciones", personaId).with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planEstudioId\": " + planEstudioId + "}"));
    }

    private List<String> roles(String username, String password) throws Exception {
        String respuesta = login(username, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jwtDecoder.decode(JsonPath.read(respuesta, "$.accessToken")).getClaimAsStringList("roles");
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private Long insertarCarrera(String nombre) {
        jdbc.update("""
                INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera)
                VALUES (?, '5 años', 'Ingeniero', 'PRESENCIAL')
                """, nombre);
        return ultimoId();
    }

    private Long insertarPlan(Long carreraId, String nombre) {
        jdbc.update("INSERT INTO plan_estudio (carrera_id, nombre, anio_inicio) VALUES (?, ?, 2024)", carreraId, nombre);
        return ultimoId();
    }

    private Long ultimoId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
