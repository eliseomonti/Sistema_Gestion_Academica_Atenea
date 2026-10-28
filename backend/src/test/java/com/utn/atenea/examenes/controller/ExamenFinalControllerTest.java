package com.utn.atenea.examenes.controller;

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

import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ABM de mesas de examen final contra la base real; {@code @Transactional} deshace todo. Lo de otros
 * módulos (carreras, personas, comisiones, inscripciones) se inserta con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExamenFinalControllerTest {

    private static final String URL = "/api/admin/examenes-finales";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long carreraId;
    Long plan;
    Long algebra;
    Long analisis;
    Long deOtraCarrera;
    Long turno;           // inscripción del 01/07 al 05/07/2026
    Long periodoCursada;
    Long ruiz;
    Long paz;
    Long dadoDeBaja;
    Long aula;
    Long aulaDadaDeBaja;

    @BeforeEach
    void crearDatos() {
        carreraId = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba mesas', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carreraId);
        algebra = insertarMateria(plan, "Álgebra de prueba mesas");
        analisis = insertarMateria(plan, "Análisis de prueba mesas");
        Long otraCarrera = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Otra carrera de prueba mesas', '2 años', 'Técnico', 'PRESENCIAL')");
        deOtraCarrera = insertarMateria(
                insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", otraCarrera),
                "Química de prueba mesas");

        turno = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', 2026, '2026-07-01 00:00', '2026-07-05 00:00')
                """, carreraId);
        periodoCursada = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', 2026, 'PRIMER_CUATRIMESTRE', '2026-02-01 00:00', '2026-03-01 00:00')
                """, carreraId);

        ruiz = insertarDocente("Ana", "Ruiz", "30000401", 980401L);
        paz = insertarDocente("Luis", "Paz", "30000402", 980402L);
        dadoDeBaja = insertarDocente("Eva", "Sosa", "30000403", 980403L);
        jdbc.update("UPDATE persona SET activo = FALSE, fecha_baja = CURRENT_DATE WHERE id = ?", dadoDeBaja);

        aula = insertar("INSERT INTO aula (nombre) VALUES ('Aula prueba mesas')");
        aulaDadaDeBaja = insertar("INSERT INTO aula (nombre, activa, fecha_baja) "
                + "VALUES ('Aula baja prueba mesas', FALSE, CURRENT_DATE)");
    }

    // --- Alta ---

    @Test
    void altaDevuelveLaMesaConLosNombresYElTribunalPorApellido() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", aula, ruiz, paz)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodoId").value(turno))
                .andExpect(jsonPath("$.materiaId").value(algebra))
                .andExpect(jsonPath("$.nombreMateria").value("Álgebra de prueba mesas"))
                .andExpect(jsonPath("$.fecha").value("2026-07-10"))
                .andExpect(jsonPath("$.horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.horaFin").value("12:00:00"))
                .andExpect(jsonPath("$.aulaId").value(aula))
                .andExpect(jsonPath("$.nombreAula").value("Aula prueba mesas"))
                .andExpect(jsonPath("$.docentes[*].apellido").value(contains("Paz", "Ruiz")));
    }

    @Test
    void sinAulaLaMesaEsVirtual() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", null, ruiz)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aulaId").doesNotExist())
                .andExpect(jsonPath("$.nombreAula").doesNotExist());
    }

    @Test
    void unPeriodoDeCursadaNoEsUnTurnoDeFinales() throws Exception {
        altaEnPeriodo(periodoCursada, algebra, "2026-07-10", "09:00", "12:00", null, ruiz)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el turno de finales con id " + periodoCursada));
    }

    @Test
    void unaMateriaDeOtraCarreraDevuelve409() throws Exception {
        alta(deOtraCarrera, "2026-07-10", "09:00", "12:00", null, ruiz)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La materia " + deOtraCarrera + " no es de la carrera del turno"));
    }

    @Test
    void laMesaNoPuedeSerAntesDelCierreDeLaInscripcion() throws Exception {
        alta(algebra, "2026-07-04", "09:00", "12:00", null, ruiz)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "La mesa tiene que ser después del cierre de la inscripción del turno (05/07/2026 00:00)"));
    }

    @Test
    void dosMesasDeLaMismaMateriaALaMismaHoraDevuelve409() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", null, ruiz).andExpect(status().isCreated());

        alta(algebra, "2026-07-10", "09:00", "11:00", null, paz)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya hay una mesa de esta materia el 10/07/2026 09:00"));
    }

    @Test
    void elAulaNoPuedeTenerDosMesasQueSePisan() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", aula, ruiz).andExpect(status().isCreated());

        alta(analisis, "2026-07-10", "11:00", "13:00", aula, paz)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El aula Aula prueba mesas ya tiene otra mesa el 10/07/2026 11:00"));
        // Las que se tocan no chocan.
        alta(analisis, "2026-07-10", "12:00", "14:00", aula, paz).andExpect(status().isCreated());
    }

    @Test
    void unAulaDadaDeBajaDevuelve409() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", aulaDadaDeBaja, ruiz)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El aula Aula baja prueba mesas está dada de baja"));
    }

    @Test
    void unAulaInexistenteDevuelve404() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", -1L, ruiz)
                .andExpect(status().isNotFound());
    }

    @Test
    void unDocenteDadoDeBajaDevuelve409() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", null, ruiz, dadoDeBaja)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El docente Sosa, Eva está dado de baja"));
    }

    @Test
    void unDocenteRepetidoDevuelve409() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", null, ruiz, ruiz)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El docente " + ruiz + " está más de una vez"));
    }

    @Test
    void unDocenteInexistenteDevuelve404() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", null, -1L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el docente con id -1"));
    }

    @Test
    void sinTribunalDevuelve400() throws Exception {
        alta(algebra, "2026-07-10", "09:00", "12:00", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("docenteIds"));
    }

    @Test
    void laHoraDeFinAntesDeLaDeInicioDevuelve400() throws Exception {
        alta(algebra, "2026-07-10", "12:00", "09:00", null, ruiz)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("horaFinPosterior"));
    }

    // --- Listado y detalle ---

    @Test
    void listarDevuelveLasMesasDelTurnoPorFechaYHora() throws Exception {
        alta(analisis, "2026-07-11", "09:00", "12:00", null, ruiz).andExpect(status().isCreated());
        alta(algebra, "2026-07-10", "14:00", "16:00", null, ruiz).andExpect(status().isCreated());
        alta(analisis, "2026-07-10", "09:00", "12:00", null, paz).andExpect(status().isCreated());

        mvc.perform(get(URL).with(admin()).param("periodoId", turno.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreMateria").value(contains(
                        "Análisis de prueba mesas", "Álgebra de prueba mesas", "Análisis de prueba mesas")))
                .andExpect(jsonPath("$[0].docentes[0].apellido").value("Paz"));
    }

    @Test
    void listarSinTurnoDevuelve400() throws Exception {
        mvc.perform(get(URL).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("periodoId"));
    }

    @Test
    void detalleDeUnaMesaInexistenteDevuelve404() throws Exception {
        mvc.perform(get(URL + "/{id}", -1L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la mesa de examen con id -1"));
    }

    // --- Modificación ---

    @Test
    void modificarReprogramaYReemplazaAulaYTribunal() throws Exception {
        Long mesa = idDe(alta(algebra, "2026-07-10", "09:00", "12:00", null, ruiz));

        modificar(mesa, "2026-07-12", "14:00", "17:00", aula, paz)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-07-12"))
                .andExpect(jsonPath("$.horaInicio").value("14:00:00"))
                .andExpect(jsonPath("$.nombreAula").value("Aula prueba mesas"))
                .andExpect(jsonPath("$.docentes[*].apellido").value(contains("Paz")));
        mvc.perform(get(URL + "/{id}", mesa).with(admin()))
                .andExpect(jsonPath("$.fecha").value("2026-07-12"))
                .andExpect(jsonPath("$.docentes[*].apellido").value(contains("Paz")));
    }

    @Test
    void modificarSinCambiarElHorarioNoChocaConsigoMisma() throws Exception {
        Long mesa = idDe(alta(algebra, "2026-07-10", "09:00", "12:00", aula, ruiz));

        modificar(mesa, "2026-07-10", "09:00", "12:00", aula, ruiz, paz)
                .andExpect(status().isOk());
    }

    // --- Baja ---

    @Test
    void eliminarBorraLaMesa() throws Exception {
        Long mesa = idDe(alta(algebra, "2026-07-10", "09:00", "12:00", null, ruiz));

        mvc.perform(delete(URL + "/{id}", mesa).with(admin())).andExpect(status().isNoContent());
        mvc.perform(get(URL + "/{id}", mesa).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void unaMesaConInscriptosNoSeBorra() throws Exception {
        Long mesa = idDe(alta(algebra, "2026-07-10", "09:00", "12:00", null, ruiz));
        Long estudiante = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', 'Díaz', '40000401', '2233333333', '2005-01-01', 'sol@mesas.test', 980404)
                """);
        Long inscripcionCarrera = insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) "
                + "VALUES (?, ?)", estudiante, plan);
        // estudiante_inscripcion_examen_final es del módulo inscripciones.
        insertar("INSERT INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id) "
                + "VALUES (?, ?)", inscripcionCarrera, mesa);

        mvc.perform(delete(URL + "/{id}", mesa).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "La mesa de examen " + mesa + " tiene inscriptos: no se puede borrar, sólo reprogramar"));
    }

    // --- Seguridad ---

    @Test
    void unEstudianteNoAdministraMesas() throws Exception {
        mvc.perform(get(URL).param("periodoId", turno.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
                .andExpect(status().isForbidden());
    }

    // --- Ayudas ---

    private ResultActions alta(Long materiaId, String fecha, String horaInicio, String horaFin, Long aulaId,
                               Long... docenteIds) throws Exception {
        return altaEnPeriodo(turno, materiaId, fecha, horaInicio, horaFin, aulaId, docenteIds);
    }

    private ResultActions altaEnPeriodo(Long periodoId, Long materiaId, String fecha, String horaInicio,
                                        String horaFin, Long aulaId, Long... docenteIds) throws Exception {
        return mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                {"periodoId": %d, "materiaId": %d, "fecha": "%s", "horaInicio": "%s", "horaFin": "%s",
                 "aulaId": %s, "docenteIds": %s}
                """.formatted(periodoId, materiaId, fecha, horaInicio, horaFin, aulaId, json(docenteIds))));
    }

    private ResultActions modificar(Long mesa, String fecha, String horaInicio, String horaFin, Long aulaId,
                                    Long... docenteIds) throws Exception {
        return mvc.perform(put(URL + "/{id}", mesa).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                {"fecha": "%s", "horaInicio": "%s", "horaFin": "%s", "aulaId": %s, "docenteIds": %s}
                """.formatted(fecha, horaInicio, horaFin, aulaId, json(docenteIds))));
    }

    private static String json(Long... ids) {
        return Arrays.toString(ids);
    }

    private static Long idDe(ResultActions alta) throws Exception {
        String json = alta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private Long insertarMateria(Long planId, String nombre) {
        Long materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES (?, 4, 64)", nombre);
        insertar("INSERT INTO plan_materia (plan_estudio_id, materia_id, anio_cursado, periodo_lectivo) "
                + "VALUES (?, ?, 1, 'PRIMER_CUATRIMESTRE')", planId, materia);
        return materia;
    }

    private Long insertarDocente(String nombre, String apellido, String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES (?, ?, ?, '2231111111', '1980-01-01', ?, ?)
                """, nombre, apellido, dni, dni + "@mesas.test", legajo);
    }

    private Long insertar(String sql, Object... parametros) {
        jdbc.update(sql, parametros);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
