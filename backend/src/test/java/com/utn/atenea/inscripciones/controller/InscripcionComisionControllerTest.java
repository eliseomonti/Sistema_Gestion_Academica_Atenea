package com.utn.atenea.inscripciones.controller;

import com.jayway.jsonpath.JsonPath;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inscripción del estudiante a materias de comisión, contra la base real; {@code @Transactional}
 * deshace todo. Lo de otros módulos (carreras, personas, comisiones) se inserta con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InscripcionComisionControllerTest {

    private static final String URL = "/api/estudiante/inscripciones-comision";
    private static final int ANIO = LocalDate.now().getYear();

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    Long carreraId;
    Long plan;
    Long programacion;
    Long analisis;
    Long estudiante;
    Long inscripcionCarreraEstudiante;
    Long otroEstudiante;
    Long sinInscripcionAlPlan;
    Long comision1;
    Long programacionEnComision1;   // 1° cuatrimestre
    Long inglesEnComision1;         // 1° cuatrimestre, cupo 1
    Long analisisEnComision1;       // 2° cuatrimestre
    Long programacionEnComision2;

    @BeforeEach
    void crearDatos() {
        carreraId = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba inscripciones', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carreraId);
        programacion = insertarMateria("Programación I de prueba", "PRIMER_CUATRIMESTRE");
        Long ingles = insertarMateria("Inglés I de prueba", "PRIMER_CUATRIMESTRE");
        analisis = insertarMateria("Análisis de prueba", "SEGUNDO_CUATRIMESTRE");

        estudiante = insertarEstudiante("40000101", 980101L);
        otroEstudiante = insertarEstudiante("40000102", 980102L);
        sinInscripcionAlPlan = insertarEstudiante("40000103", 980103L);
        inscripcionCarreraEstudiante = insertar(
                "INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)", estudiante, plan);
        insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)", otroEstudiante, plan);

        comision1 = insertarComision(1);
        programacionEnComision1 = insertarComisionMateria(comision1, programacion, "PRIMER_CUATRIMESTRE", null);
        inglesEnComision1 = insertarComisionMateria(comision1, ingles, "PRIMER_CUATRIMESTRE", 1);
        analisisEnComision1 = insertarComisionMateria(comision1, analisis, "SEGUNDO_CUATRIMESTRE", null);
        programacionEnComision2 = insertarComisionMateria(insertarComision(2), programacion, "PRIMER_CUATRIMESTRE", null);
    }

    // --- Inscripción ---

    @Test
    void inscribeAlEstudianteConLosDatosDeLaComision() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");

        inscribir(estudiante, programacionEnComision1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comisionMateriaId").value(programacionEnComision1))
                .andExpect(jsonPath("$.nombreComision").value("1/" + String.format("%02d", ANIO % 100)))
                .andExpect(jsonPath("$.nombreMateria").value("Programación I de prueba"))
                .andExpect(jsonPath("$.periodoLectivo").value("PRIMER_CUATRIMESTRE"))
                .andExpect(jsonPath("$.anioLectivo").value(ANIO))
                .andExpect(jsonPath("$.fechaInscripcion").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.estado").value("INSCRIPTO"));
    }

    @Test
    void materiaDeComisionInexistenteDevuelve404() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");

        inscribir(estudiante, -1L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la materia de comisión con id -1"));
    }

    @Test
    void sinInscripcionActivaAlPlanDevuelve409() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");

        inscribir(sinInscripcionAlPlan, programacionEnComision1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "No tenés una inscripción activa al plan de estudio de esta comisión"));
    }

    @Test
    void comisionDadaDeBajaDevuelve409() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        jdbc.update("UPDATE comision SET activa = FALSE, fecha_baja = CURRENT_DATE WHERE id = ?", comision1);

        inscribir(estudiante, programacionEnComision1)
                .andExpect(status().isConflict());
    }

    @Test
    void sinPeriodoAbiertoDevuelve409() throws Exception {
        inscribir(estudiante, programacionEnComision1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No hay un período de inscripción abierto para esta materia"));
    }

    @Test
    void elPeriodoDelPrimerCuatrimestreNoAdmiteMateriasDelSegundo() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");

        inscribir(estudiante, analisisEnComision1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No hay un período de inscripción abierto para esta materia"));
    }

    @Test
    void periodoDeshabilitadoNoAdmiteInscripciones() throws Exception {
        Long periodo = abrirPeriodo("PRIMER_CUATRIMESTRE");
        jdbc.update("UPDATE periodo_inscripcion SET habilitada = FALSE WHERE id = ?", periodo);

        inscribir(estudiante, programacionEnComision1)
                .andExpect(status().isConflict());
    }

    @Test
    void yaInscriptoDevuelve409() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        inscribir(estudiante, programacionEnComision1).andExpect(status().isCreated());

        inscribir(estudiante, programacionEnComision1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Ya estás inscripto a esta materia en la comisión 1/" + String.format("%02d", ANIO % 100)));
    }

    @Test
    void inscriptoALaMismaMateriaEnOtraComisionDevuelve409() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        inscribir(estudiante, programacionEnComision1).andExpect(status().isCreated());

        inscribir(estudiante, programacionEnComision2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya estás inscripto a esta materia en otra comisión de " + ANIO));
    }

    @Test
    void sinCupoDevuelve409() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        inscribir(otroEstudiante, inglesEnComision1).andExpect(status().isCreated());

        inscribir(estudiante, inglesEnComision1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "No hay cupo en la comisión 1/" + String.format("%02d", ANIO % 100)));
    }

    @Test
    void laBajaLiberaElCupo() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        Long inscripcion = idDe(inscribir(otroEstudiante, inglesEnComision1));
        darDeBaja(otroEstudiante, inscripcion).andExpect(status().isOk());

        inscribir(estudiante, inglesEnComision1).andExpect(status().isCreated());
    }

    @Test
    void volverAInscribirseDespuesDeLaBajaReactivaLaMismaInscripcion() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        Long inscripcion = idDe(inscribir(estudiante, programacionEnComision1));
        darDeBaja(estudiante, inscripcion).andExpect(status().isOk());

        inscribir(estudiante, programacionEnComision1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(inscripcion))
                .andExpect(jsonPath("$.estado").value("INSCRIPTO"));
    }

    @Test
    void requestSinMateriaDevuelve400() throws Exception {
        mvc.perform(post(URL).with(estudianteLogueado(estudiante)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("comisionMateriaId"));
    }

    // --- Baja ---

    @Test
    void bajaDejaLaInscripcionEnBaja() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        Long inscripcion = idDe(inscribir(estudiante, programacionEnComision1));

        darDeBaja(estudiante, inscripcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BAJA"))
                .andExpect(jsonPath("$.nombreMateria").value("Programación I de prueba"));
    }

    @Test
    void bajaDeLaInscripcionDeOtroEstudianteDevuelve404() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        Long inscripcion = idDe(inscribir(otroEstudiante, programacionEnComision1));

        darDeBaja(estudiante, inscripcion)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la inscripción a comisión con id " + inscripcion));
    }

    @Test
    void bajaDosVecesDevuelve409() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        Long inscripcion = idDe(inscribir(estudiante, programacionEnComision1));
        darDeBaja(estudiante, inscripcion).andExpect(status().isOk());

        darDeBaja(estudiante, inscripcion).andExpect(status().isConflict());
    }

    @Test
    void bajaConElPeriodoCerradoDevuelve409() throws Exception {
        Long periodo = abrirPeriodo("PRIMER_CUATRIMESTRE");
        Long inscripcion = idDe(inscribir(estudiante, programacionEnComision1));
        jdbc.update("UPDATE periodo_inscripcion SET fecha_cierre = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1), periodo);
        // El período quedó cargado por la inscripción, y el UPDATE por SQL no lo cambia en memoria.
        entityManager.clear();

        darDeBaja(estudiante, inscripcion)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El período de inscripción ya cerró: la baja de la cursada se pide en la institución"));
    }

    // --- Situación académica ---

    @Test
    void sinLasCorrelativasParaCursarDevuelve409ConLasQueFaltan() throws Exception {
        abrirPeriodo("ANUAL");
        exigirParaCursar(analisis, programacion, "CURSADA");

        inscribir(estudiante, analisisEnComision1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Te faltan correlativas para cursar esta materia: Programación I de prueba"));
    }

    @Test
    void conLaCorrelativaRegularSeInscribe() throws Exception {
        abrirPeriodo("ANUAL");
        exigirParaCursar(analisis, programacion, "CURSADA");
        cursoProgramacionElAnioPasado("REGULAR", LocalDate.now().plusYears(1));

        inscribir(estudiante, analisisEnComision1).andExpect(status().isCreated());
    }

    @Test
    void laRegularidadVencidaNoCumpleLaCorrelativa() throws Exception {
        abrirPeriodo("ANUAL");
        exigirParaCursar(analisis, programacion, "CURSADA");
        cursoProgramacionElAnioPasado("REGULAR", LocalDate.now().minusDays(1));

        inscribir(estudiante, analisisEnComision1).andExpect(status().isConflict());
    }

    @Test
    void unaMateriaQueYaTieneRegularDevuelve409() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        cursoProgramacionElAnioPasado("REGULAR", LocalDate.now().plusYears(1));

        inscribir(estudiante, programacionEnComision1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya tenés esta materia regular"));
    }

    @Test
    void ofertaMuestraSoloLoQuePuedeCursar() throws Exception {
        abrirPeriodo("ANUAL");
        exigirParaCursar(analisis, programacion, "CURSADA");

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[*].nombreMateria").value(contains("Inglés I de prueba", "Programación I de prueba")));

        // Con Programación regular, ya no la cursa y se le habilita Análisis.
        cursoProgramacionElAnioPasado("REGULAR", LocalDate.now().plusYears(1));

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[*].nombreMateria").value(contains("Inglés I de prueba", "Análisis de prueba")));
    }

    @Test
    void ofertaSigueMostrandoLoQueCursaAunqueYaNoCumplaLasCorrelativas() throws Exception {
        abrirPeriodo("ANUAL");
        inscribir(estudiante, analisisEnComision1).andExpect(status().isCreated());
        exigirParaCursar(analisis, programacion, "CURSADA");

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[*].nombreMateria").value(
                        contains("Inglés I de prueba", "Programación I de prueba", "Análisis de prueba")));
    }

    // --- Listado ---

    @Test
    void listarDevuelveSoloLasVigentesDelEstudianteEnElAnio() throws Exception {
        abrirPeriodo("ANUAL");
        inscribir(estudiante, programacionEnComision1).andExpect(status().isCreated());
        inscribir(estudiante, analisisEnComision1).andExpect(status().isCreated());
        Long dadaDeBaja = idDe(inscribir(estudiante, inglesEnComision1));
        darDeBaja(estudiante, dadaDeBaja).andExpect(status().isOk());
        inscribir(otroEstudiante, programacionEnComision2).andExpect(status().isCreated());

        mvc.perform(get(URL).with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].comisionMateriaId").value(
                        contains(programacionEnComision1.intValue(), analisisEnComision1.intValue())));
        mvc.perform(get(URL).with(estudianteLogueado(estudiante)).param("anioLectivo", String.valueOf(ANIO - 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- Oferta ---

    @Test
    void ofertaAgrupaPorMateriaLasComisionesQueAdmiteElPeriodoAbierto() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        Long aula = insertar("INSERT INTO aula (nombre) VALUES ('Aula prueba oferta')");
        insertar("INSERT INTO comision_horario (comision_materia_id, dia_semana, hora_inicio, hora_fin, aula_id) "
                + "VALUES (?, 'MARTES', '08:30', '10:30', ?)", programacionEnComision1, aula);
        insertar("INSERT INTO comision_horario (comision_materia_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, 'LUNES', '18:00', '20:00')", programacionEnComision1);
        Long docente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Ana', 'Ruiz', '30000101', '2231111111', '1980-01-01', 'ana@inscripciones.test', 980201)
                """);
        insertar("INSERT INTO comision_docente (comision_materia_id, docente_id, rol) VALUES (?, ?, 'TITULAR')",
                programacionEnComision1, docente);

        // Análisis es del 2° cuatrimestre: la ventana del 1° no la admite.
        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreMateria").value(contains("Inglés I de prueba", "Programación I de prueba")))
                .andExpect(jsonPath("$[1].planEstudioId").value(plan))
                .andExpect(jsonPath("$[1].anioCursado").value(1))
                .andExpect(jsonPath("$[1].periodoLectivo").value("PRIMER_CUATRIMESTRE"))
                .andExpect(jsonPath("$[1].comisiones[*].comisionMateriaId").value(
                        contains(programacionEnComision1.intValue(), programacionEnComision2.intValue())))
                .andExpect(jsonPath("$[1].comisiones[0].turno").value("MANANA"))
                .andExpect(jsonPath("$[1].comisiones[0].horarios[*].diaSemana").value(contains("LUNES", "MARTES")))
                .andExpect(jsonPath("$[1].comisiones[0].horarios[0].aula").doesNotExist())
                .andExpect(jsonPath("$[1].comisiones[0].horarios[1].aula").value("Aula prueba oferta"))
                .andExpect(jsonPath("$[1].comisiones[0].docentes[0].apellido").value("Ruiz"))
                .andExpect(jsonPath("$[1].comisiones[0].docentes[0].rol").value("TITULAR"))
                .andExpect(jsonPath("$[1].comisiones[0].cupoDisponible").doesNotExist())
                .andExpect(jsonPath("$[1].comisiones[0].inscripto").value(false));
    }

    @Test
    void ofertaDescuentaElCupoYMarcaDondeEstaInscripto() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[0].comisiones[0].cupoMaximo").value(1))
                .andExpect(jsonPath("$[0].comisiones[0].cupoDisponible").value(1));

        inscribir(estudiante, inglesEnComision1).andExpect(status().isCreated());

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[0].comisiones[0].cupoDisponible").value(0))
                .andExpect(jsonPath("$[0].comisiones[0].inscripto").value(true));
        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(otroEstudiante)))
                .andExpect(jsonPath("$[0].comisiones[0].cupoDisponible").value(0))
                .andExpect(jsonPath("$[0].comisiones[0].inscripto").value(false));
    }

    @Test
    void ofertaNoMuestraComisionesDadasDeBaja() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");
        jdbc.update("UPDATE comision SET activa = FALSE, fecha_baja = CURRENT_DATE WHERE id = ?", comision1);

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[*].nombreMateria").value(contains("Programación I de prueba")))
                .andExpect(jsonPath("$[0].comisiones[*].comisionMateriaId").value(
                        contains(programacionEnComision2.intValue())));
    }

    @Test
    void sinPeriodoAbiertoLaOfertaEstaVacia() throws Exception {
        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void sinInscripcionAlPlanLaOfertaEstaVacia() throws Exception {
        abrirPeriodo("PRIMER_CUATRIMESTRE");

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(sinInscripcionAlPlan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- Seguridad ---

    @Test
    void unAdminNoUsaElAutoservicio() throws Exception {
        mvc.perform(get(URL).with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        new UsuarioPrincipal(1L, estudiante, "admin"), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
                .andExpect(status().isForbidden());
    }

    // --- Ayudas ---

    private ResultActions inscribir(Long personaId, Long comisionMateriaId) throws Exception {
        return mvc.perform(post(URL).with(estudianteLogueado(personaId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"comisionMateriaId\": %d}".formatted(comisionMateriaId)));
    }

    private ResultActions darDeBaja(Long personaId, Long inscripcionId) throws Exception {
        return mvc.perform(post(URL + "/{id}/baja", inscripcionId).with(estudianteLogueado(personaId)));
    }

    private static Long idDe(ResultActions alta) throws Exception {
        String json = alta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Período de cursada de la carrera abierto desde ayer hasta mañana. */
    private Long abrirPeriodo(String periodoLectivo) {
        return insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', ?, ?, ?, ?)
                """, carreraId, ANIO, periodoLectivo, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
    }

    // correlatividad_materias es del módulo carreras.
    private void exigirParaCursar(Long materia, Long correlativa, String estado) {
        jdbc.update("""
                INSERT INTO correlatividad_materias (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id,
                                                     estado_para_cursar)
                SELECT ?, pm.id, pc.id, ? FROM plan_materia pm, plan_materia pc
                WHERE pm.plan_estudio_id = ? AND pm.materia_id = ? AND pc.plan_estudio_id = ? AND pc.materia_id = ?
                """, plan, estado, plan, materia, plan, correlativa);
    }

    /** El estudiante cursó Programación I el año pasado y el acta de cierre quedó en esa condición. */
    private void cursoProgramacionElAnioPasado(String condicion, LocalDate vencimiento) {
        Long comision = insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, 1, 'MANANA', ?)", plan, ANIO - 1);
        Long periodo = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', ?, 'PRIMER_CUATRIMESTRE', ?, ?)
                """, carreraId, ANIO - 1, LocalDateTime.now().minusYears(1), LocalDateTime.now().minusYears(1).plusDays(7));
        Long inscripcion = insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, "
                + "comision_materia_id, periodo_id) VALUES (?, ?, ?)", inscripcionCarreraEstudiante,
                insertarComisionMateria(comision, programacion, "PRIMER_CUATRIMESTRE", null), periodo);
        Long docente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Luis', 'Paz', '30000111', '2232222222', '1975-01-01', 'luis@inscripciones.test', 980211)
                """);
        // cierre_cursada es del módulo examenes.
        insertar("INSERT INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, fecha_cierre, "
                + "fecha_vencimiento) VALUES (?, ?, ?, ?, ?)",
                inscripcion, docente, condicion, LocalDate.now().minusMonths(6), vencimiento);
    }

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
                """, dni, dni + "@inscripciones.test", legajo);
    }

    private Long insertarComision(int nroComision) {
        return insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, ?, 'MANANA', ?)", plan, nroComision, ANIO);
    }

    private Long insertarComisionMateria(Long comisionId, Long materiaId, String periodoLectivo, Integer cupo) {
        return insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo, cupo_maximo) "
                + "VALUES (?, ?, ?, ?)", comisionId, materiaId, periodoLectivo, cupo);
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
