package com.utn.atenea.examenes.controller;

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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notas del estudiante, contra la base real; {@code @Transactional} deshace todo. Lo de otros módulos
 * (carreras, personas, comisiones, inscripciones) y las notas se insertan con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotasEstudianteControllerTest {

    private static final String URL = "/api/estudiante/notas";
    private static final int ANIO = LocalDate.now().getYear();
    private static final LocalDate HOY = LocalDate.now();

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long carreraId;
    Long plan;
    Long algebra;
    Long analisis;
    Long estudiante;
    Long inscripcionCarrera;
    Long otroEstudiante;
    Long inscripcionCarreraDelOtro;
    Long docente;
    Long periodoCursada;
    Long algebraEsteAnio;
    Long analisisEsteAnio;
    Long algebraElAnioPasado;

    @BeforeEach
    void crearDatos() {
        carreraId = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba notas', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carreraId);
        algebra = insertarMateria("Álgebra de prueba notas", "PRIMER_CUATRIMESTRE");
        analisis = insertarMateria("Análisis de prueba notas", "SEGUNDO_CUATRIMESTRE");

        estudiante = insertarEstudiante("40000601", 980601L);
        inscripcionCarrera = insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)",
                estudiante, plan);
        otroEstudiante = insertarEstudiante("40000602", 980602L);
        inscripcionCarreraDelOtro = insertar(
                "INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)", otroEstudiante, plan);
        docente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Ana', 'Ruiz', '30000601', '2231111111', '1980-01-01', 'ana@notas.test', 980611)
                """);

        periodoCursada = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', ?, 'ANUAL', ?, ?)
                """, carreraId, ANIO, LocalDateTime.now().minusMonths(8), LocalDateTime.now().minusMonths(7));
        Long comisionEsteAnio = insertarComision(ANIO);
        algebraEsteAnio = insertarComisionMateria(comisionEsteAnio, algebra, "PRIMER_CUATRIMESTRE");
        analisisEsteAnio = insertarComisionMateria(comisionEsteAnio, analisis, "SEGUNDO_CUATRIMESTRE");
        algebraElAnioPasado = insertarComisionMateria(insertarComision(ANIO - 1), algebra, "PRIMER_CUATRIMESTRE");
    }

    // --- Cursadas ---

    @Test
    void cursadasMuestraTodasLasEvaluacionesConLaNotaVigenteYElActa() throws Exception {
        Long cursada = inscribirACursada(inscripcionCarrera, algebraEsteAnio);
        Long delOtro = inscribirACursada(inscripcionCarreraDelOtro, algebraEsteAnio);
        Long primerParcial = insertarEvaluacion("Primer parcial", HOY.minusDays(60), null);
        Long recuperatorio = insertarEvaluacion("Recuperatorio primer parcial", HOY.minusDays(45), primerParcial);
        insertarEvaluacion("Segundo parcial", HOY.plusDays(10), null);

        // El 2 se corrigió por un 3: vale el 3.
        Long original = insertarNotaEvaluacion(primerParcial, cursada, 2, null);
        insertarNotaEvaluacion(primerParcial, cursada, 3, original);
        insertarNotaEvaluacion(recuperatorio, cursada, 7, null);
        insertarNotaEvaluacion(primerParcial, delOtro, 9, null);
        insertar("INSERT INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, nota_cursada, "
                + "fecha_cierre, fecha_vencimiento) VALUES (?, ?, 'REGULAR', 7, ?, ?)",
                cursada, docente, HOY.minusDays(30), HOY.plusYears(2));

        mvc.perform(get(URL + "/cursadas").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inscripcionComisionId").value(cursada))
                .andExpect(jsonPath("$[0].nombreMateria").value("Álgebra de prueba notas"))
                .andExpect(jsonPath("$[0].nombreComision").value("1/" + String.format("%02d", ANIO % 100)))
                .andExpect(jsonPath("$[0].anioLectivo").value(ANIO))
                .andExpect(jsonPath("$[0].evaluaciones[*].nombre").value(
                        contains("Primer parcial", "Recuperatorio primer parcial", "Segundo parcial")))
                .andExpect(jsonPath("$[0].evaluaciones[0].tipo").value("PARCIAL"))
                .andExpect(jsonPath("$[0].evaluaciones[0].condicion").value("PRESENTE"))
                .andExpect(jsonPath("$[0].evaluaciones[0].nota").value(3))
                .andExpect(jsonPath("$[0].evaluaciones[0].evaluacionRecuperadaId").doesNotExist())
                .andExpect(jsonPath("$[0].evaluaciones[1].evaluacionRecuperadaId").value(primerParcial))
                .andExpect(jsonPath("$[0].evaluaciones[1].nota").value(7))
                // El segundo parcial todavía no se tomó: viene sin nota.
                .andExpect(jsonPath("$[0].evaluaciones[2].condicion").doesNotExist())
                .andExpect(jsonPath("$[0].evaluaciones[2].nota").doesNotExist())
                .andExpect(jsonPath("$[0].cierre.condicion").value("REGULAR"))
                .andExpect(jsonPath("$[0].cierre.nota").value(7))
                .andExpect(jsonPath("$[0].cierre.fechaVencimiento").value(HOY.plusYears(2).toString()));
    }

    @Test
    void unaNotaAusenteVieneSinNota() throws Exception {
        Long cursada = inscribirACursada(inscripcionCarrera, algebraEsteAnio);
        Long parcial = insertarEvaluacion("Primer parcial", HOY.minusDays(60), null);
        insertar("INSERT INTO nota_evaluacion (comision_materia_id, evaluacion_cursada_id, inscripcion_comision_id, "
                + "docente_id, condicion) VALUES (?, ?, ?, ?, 'AUSENTE')", algebraEsteAnio, parcial, cursada, docente);

        mvc.perform(get(URL + "/cursadas").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[0].evaluaciones[0].condicion").value("AUSENTE"))
                .andExpect(jsonPath("$[0].evaluaciones[0].nota").doesNotExist());
    }

    @Test
    void cursadasVanDelAnioMasRecienteAlMasViejoYSinActaElCierreEsNull() throws Exception {
        inscribirACursada(inscripcionCarrera, algebraElAnioPasado);
        inscribirACursada(inscripcionCarrera, analisisEsteAnio);
        inscribirACursada(inscripcionCarrera, algebraEsteAnio);

        mvc.perform(get(URL + "/cursadas").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[*].nombreMateria").value(
                        contains("Álgebra de prueba notas", "Análisis de prueba notas", "Álgebra de prueba notas")))
                .andExpect(jsonPath("$[*].anioLectivo").value(contains(ANIO, ANIO, ANIO - 1)))
                .andExpect(jsonPath("$[0].evaluaciones").isEmpty())
                .andExpect(jsonPath("$[0].cierre").doesNotExist());
    }

    @Test
    void unaCursadaDadaDeBajaNoAparece() throws Exception {
        Long cursada = inscribirACursada(inscripcionCarrera, algebraEsteAnio);
        jdbc.update("UPDATE estudiante_inscripcion_comision SET estado = 'BAJA', fecha_baja = CURRENT_DATE WHERE id = ?",
                cursada);

        mvc.perform(get(URL + "/cursadas").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- Finales ---

    @Test
    void finalesMuestraLasMesasQueYaSeTomaronConLaNotaVigente() throws Exception {
        Long turno = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', ?, ?, ?)
                """, carreraId, ANIO, LocalDateTime.now().minusDays(60), LocalDateTime.now().minusDays(55));
        Long aprobado = inscribirAFinal(insertarMesa(algebra, turno, HOY.minusDays(30)));
        Long ausente = inscribirAFinal(insertarMesa(analisis, turno, HOY.minusDays(10)));
        Long sinNota = inscribirAFinal(insertarMesa(analisis, turno, HOY.minusDays(2)));
        inscribirAFinal(insertarMesa(algebra, turno, HOY.plusDays(5)));   // todavía no se tomó: no aparece

        // El 4 se corrigió por un 6: vale el 6, que aprueba.
        Long original = insertar("INSERT INTO nota_final (inscripcion_examen_final_id, docente_id, nota) VALUES (?, ?, 4)",
                aprobado, docente);
        insertar("INSERT INTO nota_final (inscripcion_examen_final_id, docente_id, nota, nota_rectificada_id) "
                + "VALUES (?, ?, 6, ?)", aprobado, docente, original);
        insertar("INSERT INTO nota_final (inscripcion_examen_final_id, docente_id, condicion) VALUES (?, ?, 'AUSENTE')",
                ausente, docente);

        mvc.perform(get(URL + "/finales").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].inscripcionExamenFinalId").value(
                        contains(sinNota.intValue(), ausente.intValue(), aprobado.intValue())))
                .andExpect(jsonPath("$[0].nombreMateria").value("Análisis de prueba notas"))
                .andExpect(jsonPath("$[0].condicion").doesNotExist())
                .andExpect(jsonPath("$[0].aprobado").doesNotExist())
                .andExpect(jsonPath("$[1].condicion").value("AUSENTE"))
                .andExpect(jsonPath("$[1].nota").doesNotExist())
                .andExpect(jsonPath("$[1].aprobado").value(false))
                .andExpect(jsonPath("$[2].nombreMateria").value("Álgebra de prueba notas"))
                .andExpect(jsonPath("$[2].fecha").value(HOY.minusDays(30).toString()))
                .andExpect(jsonPath("$[2].nota").value(6))
                .andExpect(jsonPath("$[2].aprobado").value(true));
    }

    @Test
    void sinInscripcionesLasListasEstanVacias() throws Exception {
        mvc.perform(get(URL + "/cursadas").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get(URL + "/finales").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- Seguridad ---

    @Test
    void unAdminNoUsaElAutoservicio() throws Exception {
        mvc.perform(get(URL + "/cursadas").with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        new UsuarioPrincipal(1L, estudiante, "admin"), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
                .andExpect(status().isForbidden());
    }

    // --- Ayudas ---

    private Long insertarMateria(String nombre, String periodoLectivo) {
        Long materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES (?, 4, 64)", nombre);
        insertar("INSERT INTO plan_materia (plan_estudio_id, materia_id, anio_cursado, periodo_lectivo) "
                + "VALUES (?, ?, 1, ?)", plan, materia, periodoLectivo);
        return materia;
    }

    private Long insertarEstudiante(String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', 'Díaz', ?, '2233333333', '2005-01-01', ?, ?)
                """, dni, dni + "@notas.test", legajo);
    }

    private Long insertarComision(int anioLectivo) {
        return insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, 1, 'MANANA', ?)", plan, anioLectivo);
    }

    private Long insertarComisionMateria(Long comisionId, Long materiaId, String periodoLectivo) {
        return insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo) VALUES (?, ?, ?)",
                comisionId, materiaId, periodoLectivo);
    }

    // estudiante_inscripcion_comision y estudiante_inscripcion_examen_final son del módulo inscripciones.
    private Long inscribirACursada(Long inscripcionCarreraId, Long comisionMateriaId) {
        return insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, comision_materia_id, "
                + "periodo_id) VALUES (?, ?, ?)", inscripcionCarreraId, comisionMateriaId, periodoCursada);
    }

    private Long inscribirAFinal(Long examenFinalId) {
        return insertar("INSERT INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id, "
                + "fecha_inscripcion) VALUES (?, ?, ?)", inscripcionCarrera, examenFinalId, HOY.minusDays(58));
    }

    /** Una evaluación de Álgebra en la comisión de este año. recuperaA en null: es una instancia original. */
    private Long insertarEvaluacion(String nombre, LocalDate fecha, Long recuperaA) {
        return insertar("INSERT INTO evaluacion_cursada (comision_materia_id, nombre, fecha, evaluacion_recuperada_id) "
                + "VALUES (?, ?, ?, ?)", algebraEsteAnio, nombre, fecha, recuperaA);
    }

    private Long insertarNotaEvaluacion(Long evaluacionId, Long inscripcionComisionId, int nota, Long rectificaA) {
        return insertar("INSERT INTO nota_evaluacion (comision_materia_id, evaluacion_cursada_id, inscripcion_comision_id, "
                + "docente_id, nota, nota_rectificada_id) VALUES (?, ?, ?, ?, ?, ?)",
                algebraEsteAnio, evaluacionId, inscripcionComisionId, docente, nota, rectificaA);
    }

    private Long insertarMesa(Long materia, Long turno, LocalDate fecha) {
        return insertar("INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin) "
                + "VALUES (?, ?, ?, '09:00', '12:00')", materia, turno, fecha);
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
