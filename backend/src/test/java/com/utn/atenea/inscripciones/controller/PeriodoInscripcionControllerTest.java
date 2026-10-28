package com.utn.atenea.inscripciones.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ABM de períodos de inscripción contra la base real; {@code @Transactional} deshace todo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PeriodoInscripcionControllerTest {

    private static final String URL = "/api/admin/periodos-inscripcion";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long carreraId;
    Long otraCarreraId;

    @BeforeEach
    void crearCarreras() {
        // carrera es del módulo carreras: se inserta con SQL.
        carreraId = insertarCarrera("Carrera de prueba períodos");
        otraCarreraId = insertarCarrera("Otra carrera de prueba períodos");
    }

    // --- Alta ---

    @Test
    void altaDeCursadaLlevaPeriodoLectivo() throws Exception {
        altaCursada(carreraId, "2026-03-01T08:00:00", "2026-03-15T23:59:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carreraId").value(carreraId))
                .andExpect(jsonPath("$.tipo").value("CURSADA"))
                .andExpect(jsonPath("$.anioLectivo").value(2026))
                .andExpect(jsonPath("$.periodoLectivo").value("PRIMER_CUATRIMESTRE"))
                .andExpect(jsonPath("$.fechaInicio").value("2026-03-01T08:00:00"))
                .andExpect(jsonPath("$.fechaCierre").value("2026-03-15T23:59:00"))
                .andExpect(jsonPath("$.habilitada").value(true));
    }

    @Test
    void altaDeFinalNoLlevaPeriodoLectivo() throws Exception {
        altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("FINAL"))
                .andExpect(jsonPath("$.periodoLectivo").doesNotExist());
    }

    @Test
    void cursadaSinPeriodoLectivoDevuelve400() throws Exception {
        alta("""
                {"carreraId": %d, "tipo": "CURSADA", "anioLectivo": 2026,
                 "fechaInicio": "2026-03-01T08:00:00", "fechaCierre": "2026-03-15T23:59:00"}
                """.formatted(carreraId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("periodoLectivoSegunTipo"));
    }

    @Test
    void finalConPeriodoLectivoDevuelve400() throws Exception {
        alta("""
                {"carreraId": %d, "tipo": "FINAL", "anioLectivo": 2026, "periodoLectivo": "ANUAL",
                 "fechaInicio": "2026-02-01T08:00:00", "fechaCierre": "2026-02-07T23:59:00"}
                """.formatted(carreraId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("periodoLectivoSegunTipo"));
    }

    @Test
    void cierreAnteriorAlInicioDevuelve400() throws Exception {
        altaFinal(carreraId, "2026-02-07T08:00:00", "2026-02-01T08:00:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("fechaCierrePosterior"));
    }

    @Test
    void carreraInexistenteDevuelve404() throws Exception {
        altaFinal(-1L, "2026-02-01T08:00:00", "2026-02-07T23:59:00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la carrera con id -1"));
    }

    @Test
    void periodoDelMismoTipoSuperpuestoDevuelve409() throws Exception {
        altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00").andExpect(status().isCreated());

        altaFinal(carreraId, "2026-02-05T08:00:00", "2026-02-10T23:59:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La carrera " + carreraId
                        + " ya tiene un período de inscripción a FINAL que se superpone con esas fechas"));
    }

    @Test
    void sePuedenSuperponerDistintoTipoOtraCarreraOPeriodosContiguos() throws Exception {
        altaFinal(carreraId, "2026-03-01T08:00:00", "2026-03-15T08:00:00").andExpect(status().isCreated());

        altaCursada(carreraId, "2026-03-01T08:00:00", "2026-03-15T08:00:00").andExpect(status().isCreated());
        altaFinal(otraCarreraId, "2026-03-01T08:00:00", "2026-03-15T08:00:00").andExpect(status().isCreated());
        altaFinal(carreraId, "2026-03-15T08:00:00", "2026-03-20T08:00:00").andExpect(status().isCreated());
    }

    // --- Modificación ---

    @Test
    void modificarCambiaLasFechasAunqueSePisenConLasSuyas() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));

        modificar(id, "2026-02-03T08:00:00", "2026-02-10T23:59:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value("2026-02-03T08:00:00"))
                .andExpect(jsonPath("$.fechaCierre").value("2026-02-10T23:59:00"));
    }

    @Test
    void modificarSuperponiendoseConOtroDevuelve409() throws Exception {
        altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00").andExpect(status().isCreated());
        Long id = idDe(altaFinal(carreraId, "2026-07-01T08:00:00", "2026-07-07T23:59:00"));

        modificar(id, "2026-02-05T08:00:00", "2026-02-10T23:59:00")
                .andExpect(status().isConflict());
    }

    @Test
    void modificarInexistenteDevuelve404() throws Exception {
        modificar(-1L, "2026-02-03T08:00:00", "2026-02-10T23:59:00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el período de inscripción con id -1"));
    }

    @Test
    void elCierreDeUnTurnoNoPuedeQuedarDespuesDeSuPrimeraMesa() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));
        insertarMesa(id, "2026-02-12", "14:00");
        insertarMesa(id, "2026-02-10", "09:00");

        modificar(id, "2026-02-01T08:00:00", "2026-02-10T10:00:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El turno tiene mesas desde el 10/02/2026 09:00: la inscripción tiene que cerrar antes"));
        // Hasta el inicio de la primera mesa, sí.
        modificar(id, "2026-02-01T08:00:00", "2026-02-10T09:00:00")
                .andExpect(status().isOk());
    }

    // --- Habilitar y deshabilitar ---

    @Test
    void deshabilitarYVolverAHabilitar() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));

        mvc.perform(post(URL + "/{id}/deshabilitar", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitada").value(false));
        mvc.perform(post(URL + "/{id}/habilitar", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitada").value(true));
    }

    @Test
    void deshabilitarDosVecesDevuelve409() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));
        mvc.perform(post(URL + "/{id}/deshabilitar", id).with(admin())).andExpect(status().isOk());

        mvc.perform(post(URL + "/{id}/deshabilitar", id).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El período de inscripción " + id + " ya está deshabilitado"));
    }

    @Test
    void habilitarUnoHabilitadoDevuelve409() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));

        mvc.perform(post(URL + "/{id}/habilitar", id).with(admin()))
                .andExpect(status().isConflict());
    }

    // --- Borrado ---

    @Test
    void eliminarBorraElPeriodo() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));

        mvc.perform(delete(URL + "/{id}", id).with(admin())).andExpect(status().isNoContent());
        mvc.perform(get(URL + "/{id}", id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void eliminarConMesasDeExamenDevuelve409() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));
        // examen_final es del módulo examenes: se inserta con SQL.
        jdbc.update("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) VALUES ('Materia de prueba períodos', 4, 64)");
        jdbc.update("""
                INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin)
                VALUES (?, ?, '2026-02-15', '08:00', '12:00')
                """, ultimoId(), id);

        mvc.perform(delete(URL + "/{id}", id).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El período de inscripción " + id
                        + " tiene inscripciones o mesas de examen: no se puede borrar, sólo deshabilitar"));
    }

    // --- Listado y detalle ---

    @Test
    void listarFiltraPorCarreraAnioYTipoYOrdenaDelMasNuevoAlMasViejo() throws Exception {
        Long febrero = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));
        Long julio = idDe(altaFinal(carreraId, "2026-07-01T08:00:00", "2026-07-07T23:59:00"));
        altaCursada(carreraId, "2026-03-01T08:00:00", "2026-03-15T23:59:00").andExpect(status().isCreated());
        alta("""
                {"carreraId": %d, "tipo": "FINAL", "anioLectivo": 2025,
                 "fechaInicio": "2025-12-01T08:00:00", "fechaCierre": "2025-12-07T23:59:00"}
                """.formatted(carreraId)).andExpect(status().isCreated());
        altaFinal(otraCarreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00").andExpect(status().isCreated());

        mvc.perform(get(URL).with(admin())
                        .param("carreraId", carreraId.toString())
                        .param("anioLectivo", "2026")
                        .param("tipo", "FINAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(julio.intValue(), febrero.intValue())));
    }

    @Test
    void listarSinCarreraDevuelve400() throws Exception {
        mvc.perform(get(URL).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("carreraId"));
    }

    @Test
    void detalleDevuelveElPeriodo() throws Exception {
        Long id = idDe(altaFinal(carreraId, "2026-02-01T08:00:00", "2026-02-07T23:59:00"));

        mvc.perform(get(URL + "/{id}", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tipo").value("FINAL"));
    }

    // --- Ayudas ---

    private ResultActions altaCursada(Long carreraId, String fechaInicio, String fechaCierre) throws Exception {
        return alta("""
                {"carreraId": %d, "tipo": "CURSADA", "anioLectivo": 2026, "periodoLectivo": "PRIMER_CUATRIMESTRE",
                 "fechaInicio": "%s", "fechaCierre": "%s"}
                """.formatted(carreraId, fechaInicio, fechaCierre));
    }

    private ResultActions altaFinal(Long carreraId, String fechaInicio, String fechaCierre) throws Exception {
        return alta("""
                {"carreraId": %d, "tipo": "FINAL", "anioLectivo": 2026,
                 "fechaInicio": "%s", "fechaCierre": "%s"}
                """.formatted(carreraId, fechaInicio, fechaCierre));
    }

    private ResultActions alta(String json) throws Exception {
        return mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions modificar(Long id, String fechaInicio, String fechaCierre) throws Exception {
        return mvc.perform(put(URL + "/{id}", id).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                {"fechaInicio": "%s", "fechaCierre": "%s"}
                """.formatted(fechaInicio, fechaCierre)));
    }

    private static Long idDe(ResultActions alta) throws Exception {
        String json = alta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    // materia es de carreras y examen_final, de examenes: se insertan con SQL.
    private void insertarMesa(Long turnoId, String fecha, String horaInicio) {
        jdbc.update("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Materia de prueba períodos', 4, 64)");
        jdbc.update("INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin) "
                + "VALUES (?, ?, ?, ?, ADDTIME(?, '02:00'))", ultimoId(), turnoId, fecha, horaInicio, horaInicio);
    }

    private Long insertarCarrera(String nombre) {
        jdbc.update("""
                INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera)
                VALUES (?, '2 años', 'Técnico', 'PRESENCIAL')
                """, nombre);
        return ultimoId();
    }

    private Long ultimoId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
