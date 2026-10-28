package com.utn.atenea.inscripciones.controller;

import com.utn.atenea.seguridad.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plan de estudio del estudiante con el estado de cada materia, contra la base real; {@code @Transactional}
 * deshace todo. Lo de otros módulos (carreras, personas, comisiones, examenes) se inserta con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanEstudianteControllerTest {

    private static final String URL = "/api/estudiante/plan-estudio";
    private static final int ANIO = LocalDate.now().getYear();
    private static final LocalDate VENCIMIENTO = LocalDate.now().plusYears(1);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long carreraId;
    Long plan;
    Long estudiante;
    Long inscripcionCarrera;
    Long docente;
    Long periodoCursada;
    int nroComision;

    @BeforeEach
    void crearDatos() {
        carreraId = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba plan estudiante', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carreraId);
        estudiante = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', 'Díaz', '40000301', '2233333333', '2005-01-01', 'sol@plan-estudiante.test', 980301)
                """);
        inscripcionCarrera = insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)",
                estudiante, plan);
        docente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Ana', 'Ruiz', '30000301', '2231111111', '1980-01-01', 'ana@plan-estudiante.test', 980311)
                """);
        periodoCursada = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', ?, 'ANUAL', ?, ?)
                """, carreraId, ANIO - 1, LocalDate.now().minusYears(1).atStartOfDay(),
                LocalDate.now().minusYears(1).plusDays(7).atStartOfDay());
    }

    @Test
    void devuelveElEstadoDeCadaMateriaYElAvance() throws Exception {
        Long ingles = insertarMateria("Inglés de prueba plan", 1, "PRIMER_CUATRIMESTRE");
        Long matematica = insertarMateria("Matemática de prueba plan", 1, "PRIMER_CUATRIMESTRE");
        Long programacion = insertarMateria("Programación de prueba plan", 1, "PRIMER_CUATRIMESTRE");
        Long analisis = insertarMateria("Análisis de prueba plan", 1, "SEGUNDO_CUATRIMESTRE");
        insertarMateria("Física de prueba plan", 2, "ANUAL");

        cerrar(cursar(ingles), "REGULAR", 6, VENCIMIENTO);
        aprobarFinal(ingles, 7);
        cerrar(cursar(matematica), "PROMOCIONADO", 9, null);
        cerrar(cursar(programacion), "REGULAR", 5, VENCIMIENTO);
        cursar(analisis);

        mvc.perform(get(URL).with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planEstudioId").value(plan))
                .andExpect(jsonPath("$[0].nombreCarrera").value("Carrera de prueba plan estudiante"))
                .andExpect(jsonPath("$[0].nombrePlan").value("Plan 2024"))
                .andExpect(jsonPath("$[0].materiasAprobadas").value(2))
                .andExpect(jsonPath("$[0].totalMaterias").value(5))
                .andExpect(jsonPath("$[0].porcentajeAvance").value(40))
                .andExpect(jsonPath("$[0].materias[*].nombre").value(contains("Inglés de prueba plan",
                        "Matemática de prueba plan", "Programación de prueba plan", "Análisis de prueba plan",
                        "Física de prueba plan")))
                .andExpect(jsonPath("$[0].materias[*].estado").value(
                        contains("APROBADA", "APROBADA", "REGULAR", "CURSANDO", "NO_CURSADA")))
                // La nota es la del final o la de la promoción; la de una cursada regular no se muestra.
                .andExpect(jsonPath("$[0].materias[0].nota").value(7))
                .andExpect(jsonPath("$[0].materias[1].nota").value(9))
                .andExpect(jsonPath("$[0].materias[2].nota").doesNotExist())
                .andExpect(jsonPath("$[0].materias[2].vencimientoRegularidad").value(VENCIMIENTO.toString()))
                .andExpect(jsonPath("$[0].materias[0].vencimientoRegularidad").doesNotExist())
                .andExpect(jsonPath("$[0].materias[4].anioCursado").value(2))
                .andExpect(jsonPath("$[0].materias[4].periodoLectivo").value("ANUAL"));
    }

    @Test
    void unaInscripcionACarreraDadaDeBajaNoSeMuestra() throws Exception {
        jdbc.update("UPDATE inscripcion_carrera SET estado = 'BAJA', fecha_estado = CURRENT_DATE WHERE id = ?",
                inscripcionCarrera);

        mvc.perform(get(URL).with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void unPlanSinMateriasTieneAvanceCero() throws Exception {
        mvc.perform(get(URL).with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[0].totalMaterias").value(0))
                .andExpect(jsonPath("$[0].porcentajeAvance").value(0))
                .andExpect(jsonPath("$[0].materias").isEmpty());
    }

    @Test
    void unAdminNoUsaElAutoservicio() throws Exception {
        mvc.perform(get(URL).with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        new UsuarioPrincipal(1L, estudiante, "admin"), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
                .andExpect(status().isForbidden());
    }

    // --- Ayudas ---

    private Long insertarMateria(String nombre, int anioCursado, String periodoLectivo) {
        Long materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES (?, 4, 64)", nombre);
        insertar("INSERT INTO plan_materia (plan_estudio_id, materia_id, anio_cursado, periodo_lectivo) "
                + "VALUES (?, ?, ?, ?)", plan, materia, anioCursado, periodoLectivo);
        return materia;
    }

    /** Lo inscribe a la materia en una comisión del año pasado. Devuelve la inscripción a comisión. */
    private Long cursar(Long materia) {
        Long comision = insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, ?, 'MANANA', ?)", plan, ++nroComision, ANIO - 1);
        Long comisionMateria = insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo) "
                + "VALUES (?, ?, 'PRIMER_CUATRIMESTRE')", comision, materia);
        return insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, comision_materia_id, "
                + "periodo_id) VALUES (?, ?, ?)", inscripcionCarrera, comisionMateria, periodoCursada);
    }

    // cierre_cursada es del módulo examenes.
    private void cerrar(Long inscripcionComision, String condicion, Integer nota, LocalDate vencimiento) {
        insertar("INSERT INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, nota_cursada, "
                + "fecha_cierre, fecha_vencimiento) VALUES (?, ?, ?, ?, ?, ?)",
                inscripcionComision, docente, condicion, nota, LocalDate.now().minusMonths(6), vencimiento);
    }

    // examen_final y nota_final son del módulo examenes.
    private void aprobarFinal(Long materia, int nota) {
        Long turno = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', ?, ?, ?)
                """, carreraId, ANIO, LocalDate.now().minusMonths(1).atStartOfDay(),
                LocalDate.now().minusMonths(1).plusDays(5).atStartOfDay());
        Long mesa = insertar("INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin) "
                + "VALUES (?, ?, ?, '09:00', '12:00')", materia, turno, LocalDate.now().minusDays(20));
        Long inscripcionFinal = insertar("INSERT INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, "
                + "examen_final_id) VALUES (?, ?)", inscripcionCarrera, mesa);
        insertar("INSERT INTO nota_final (inscripcion_examen_final_id, docente_id, nota) VALUES (?, ?, ?)",
                inscripcionFinal, docente, nota);
    }

    private Long insertar(String sql, Object... parametros) {
        jdbc.update(sql, parametros);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static RequestPostProcessor estudianteLogueado(Long personaId) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new UsuarioPrincipal(1L, personaId, "estudiante"), null,
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))));
    }
}
