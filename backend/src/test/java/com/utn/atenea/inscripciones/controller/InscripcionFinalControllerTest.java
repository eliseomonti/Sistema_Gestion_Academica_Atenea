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
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inscripción del estudiante a mesas de examen final, contra la base real; {@code @Transactional}
 * deshace todo. Lo de otros módulos (carreras, personas, comisiones, examenes) se inserta con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InscripcionFinalControllerTest {

    private static final String URL = "/api/estudiante/inscripciones-final";
    private static final int ANIO = LocalDate.now().getYear();
    private static final LocalDate DIA_DE_LA_MESA = LocalDate.now().plusDays(5);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    Long carreraId;
    Long plan;
    Long algebra;
    Long analisis;
    Long estudiante;
    Long inscripcionCarrera;
    Long otroEstudiante;
    Long docente;
    Long aula;
    Long periodoCursadaAnterior;
    Long turno;   // abierto desde ayer hasta mañana
    int nroComision;

    @BeforeEach
    void crearDatos() {
        carreraId = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba finales', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carreraId);
        algebra = insertarMateria("Álgebra de prueba finales");
        analisis = insertarMateria("Análisis de prueba finales");

        estudiante = insertarEstudiante("40000501", 980501L);
        inscripcionCarrera = insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)",
                estudiante, plan);
        otroEstudiante = insertarEstudiante("40000502", 980502L);
        insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)", otroEstudiante, plan);
        docente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Ana', 'Ruiz', '30000501', '2231111111', '1980-01-01', 'ana@finales.test', 980511)
                """);
        aula = insertar("INSERT INTO aula (nombre) VALUES ('Aula prueba finales')");

        periodoCursadaAnterior = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', ?, 'ANUAL', ?, ?)
                """, carreraId, ANIO - 1, LocalDateTime.now().minusYears(1), LocalDateTime.now().minusYears(1).plusDays(7));
        turno = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', ?, ?, ?)
                """, carreraId, ANIO, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
    }

    // --- Inscripción ---

    @Test
    void inscribeAlEstudianteConLaMateriaRegular() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");

        inscribir(estudiante, mesa)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.examenFinalId").value(mesa))
                .andExpect(jsonPath("$.materiaId").value(algebra))
                .andExpect(jsonPath("$.nombreMateria").value("Álgebra de prueba finales"))
                .andExpect(jsonPath("$.fecha").value(DIA_DE_LA_MESA.toString()))
                .andExpect(jsonPath("$.horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.nombreAula").value("Aula prueba finales"))
                .andExpect(jsonPath("$.fechaInscripcion").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.estado").value("INSCRIPTO"));
    }

    @Test
    void mesaInexistenteDevuelve404() throws Exception {
        inscribir(estudiante, -1L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la mesa de examen con id -1"));
    }

    @Test
    void conElTurnoCerradoDevuelve409() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");
        jdbc.update("UPDATE periodo_inscripcion SET habilitada = FALSE WHERE id = ?", turno);

        inscribir(estudiante, mesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La inscripción al turno de esta mesa no está abierta"));
    }

    @Test
    void sinInscripcionALaCarreraDevuelve409() throws Exception {
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");

        inscribir(insertarEstudiante("40000503", 980503L), mesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "No tenés una inscripción activa a un plan de estudio con esta materia"));
    }

    @Test
    void sinLaMateriaRegularDevuelve409() throws Exception {
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");

        inscribir(estudiante, mesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Para rendir el final tenés que tener la materia regular"));
    }

    @Test
    void conLaMateriaAprobadaDevuelve409() throws Exception {
        cerrarCursada(estudiante, algebra, "PROMOCIONADO", 8, null);
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");

        inscribir(estudiante, mesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya tenés esta materia aprobada"));
    }

    @Test
    void siLaRegularidadVenceAntesDeLaMesaDevuelve409() throws Exception {
        LocalDate vencimiento = DIA_DE_LA_MESA.minusDays(1);
        regularizar(estudiante, algebra, vencimiento);
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");

        inscribir(estudiante, mesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Tu regularidad vence el "
                        + vencimiento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ", antes de la mesa"));
    }

    @Test
    void sinLasCorrelativasParaRendirDevuelve409ConLasQueFaltan() throws Exception {
        exigirParaRendir(analisis, algebra, "APROBADA");
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        regularizar(estudiante, analisis, LocalDate.now().plusYears(1));
        Long mesa = insertarMesa(analisis, DIA_DE_LA_MESA, "09:00");

        inscribir(estudiante, mesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Te faltan correlativas para rendir esta materia: Álgebra de prueba finales"));
    }

    @Test
    void inscribirseDosVecesALaMismaMesaDevuelve409() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");
        inscribir(estudiante, mesa).andExpect(status().isCreated());

        inscribir(estudiante, mesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya estás inscripto a esta mesa"));
    }

    @Test
    void enUnTurnoLaMateriaSeRindeEnUnaSolaMesa() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        inscribir(estudiante, insertarMesa(algebra, DIA_DE_LA_MESA, "09:00")).andExpect(status().isCreated());

        inscribir(estudiante, insertarMesa(algebra, DIA_DE_LA_MESA.plusDays(2), "09:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya estás inscripto a otra mesa de esta materia en el turno"));
    }

    @Test
    void despuesDeLaBajaSePuedeVolverAInscribirYSeReactivaLaMismaInscripcion() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");
        Long inscripcion = idDe(inscribir(estudiante, mesa));
        darDeBaja(estudiante, inscripcion).andExpect(status().isOk());

        inscribir(estudiante, mesa)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(inscripcion))
                .andExpect(jsonPath("$.estado").value("INSCRIPTO"));
    }

    // --- Baja ---

    @Test
    void laBajaAntesDeLaMesaDejaLaInscripcionEnBaja() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long inscripcion = idDe(inscribir(estudiante, insertarMesa(algebra, DIA_DE_LA_MESA, "09:00")));

        darDeBaja(estudiante, inscripcion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BAJA"));
        darDeBaja(estudiante, inscripcion)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La inscripción a final " + inscripcion + " ya está dada de baja"));
    }

    @Test
    void elDiaDeLaMesaYaNoSePuedeDarDeBaja() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");
        Long inscripcion = idDe(inscribir(estudiante, mesa));
        jdbc.update("UPDATE examen_final SET fecha = CURRENT_DATE WHERE id = ?", mesa);
        // La mesa quedó cargada por la inscripción, y el UPDATE por SQL no la cambia en memoria.
        entityManager.clear();

        darDeBaja(estudiante, inscripcion)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La baja de un final se hace hasta el día anterior a la mesa"));
    }

    @Test
    void laInscripcionDeOtroEstudianteDevuelve404() throws Exception {
        regularizar(otroEstudiante, algebra, LocalDate.now().plusYears(1));
        Long deOtro = idDe(inscribir(otroEstudiante, insertarMesa(algebra, DIA_DE_LA_MESA, "09:00")));

        darDeBaja(estudiante, deOtro)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la inscripción a final con id " + deOtro));
    }

    // --- Listado ---

    @Test
    void listarDevuelveLasMesasQueTodaviaNoSeTomaronPorFecha() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        regularizar(estudiante, analisis, LocalDate.now().plusYears(1));
        Long algebraMasTarde = insertarMesa(algebra, DIA_DE_LA_MESA.plusDays(1), "09:00");
        Long analisisAntes = insertarMesa(analisis, DIA_DE_LA_MESA, "14:00");
        inscribir(estudiante, algebraMasTarde).andExpect(status().isCreated());
        inscribir(estudiante, analisisAntes).andExpect(status().isCreated());
        // Una inscripción a una mesa que ya pasó no se lista.
        Long mesaPasada = insertarMesa(algebra, LocalDate.now().minusDays(30), "09:00");
        insertar("INSERT INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id, "
                + "fecha_inscripcion) VALUES (?, ?, ?)", inscripcionCarrera, mesaPasada, LocalDate.now().minusDays(40));

        mvc.perform(get(URL).with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].examenFinalId").value(
                        contains(analisisAntes.intValue(), algebraMasTarde.intValue())));
    }

    // --- Oferta ---

    @Test
    void ofertaMuestraLasMesasQuePuedeRendirConSuTribunal() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long mesaAlgebra = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");
        insertarMesa(analisis, DIA_DE_LA_MESA, "14:00");   // Análisis no la tiene regular: no aparece.

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].examenFinalId").value(contains(mesaAlgebra.intValue())))
                .andExpect(jsonPath("$[0].planEstudioId").value(plan))
                .andExpect(jsonPath("$[0].nombreMateria").value("Álgebra de prueba finales"))
                .andExpect(jsonPath("$[0].fecha").value(DIA_DE_LA_MESA.toString()))
                .andExpect(jsonPath("$[0].nombreAula").value("Aula prueba finales"))
                .andExpect(jsonPath("$[0].docentes[0].apellido").value("Ruiz"))
                .andExpect(jsonPath("$[0].inscripto").value(false));

        inscribir(estudiante, mesaAlgebra).andExpect(status().isCreated());

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[0].inscripto").value(true));
    }

    @Test
    void ofertaSigueMostrandoLaMesaEnLaQueEstaInscriptoAunqueYaNoPuedaRendir() throws Exception {
        Long cursada = regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        Long mesa = insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");
        inscribir(estudiante, mesa).andExpect(status().isCreated());
        // Después de anotarse, le rectifican el acta y la regularidad vence antes de la mesa. cierre_cursada
        // es append-only (hay triggers): corregir es insertar otra que apunta a la anterior.
        Long acta = jdbc.queryForObject("SELECT id FROM cierre_cursada WHERE inscripcion_comision_id = ?",
                Long.class, cursada);
        insertar("INSERT INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, nota_cursada, "
                + "fecha_cierre, fecha_vencimiento, cierre_rectificado_id) VALUES (?, ?, 'REGULAR', 6, ?, ?, ?)",
                cursada, docente, LocalDate.now().minusMonths(6), DIA_DE_LA_MESA.minusDays(1), acta);

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
                .andExpect(jsonPath("$[*].examenFinalId").value(contains(mesa.intValue())))
                .andExpect(jsonPath("$[0].inscripto").value(true));
    }

    @Test
    void sinTurnoAbiertoLaOfertaEstaVacia() throws Exception {
        regularizar(estudiante, algebra, LocalDate.now().plusYears(1));
        insertarMesa(algebra, DIA_DE_LA_MESA, "09:00");
        jdbc.update("UPDATE periodo_inscripcion SET habilitada = FALSE WHERE id = ?", turno);

        mvc.perform(get(URL + "/oferta").with(estudianteLogueado(estudiante)))
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

    private ResultActions inscribir(Long personaId, Long examenFinalId) throws Exception {
        return mvc.perform(post(URL).with(estudianteLogueado(personaId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"examenFinalId\": %d}".formatted(examenFinalId)));
    }

    private ResultActions darDeBaja(Long personaId, Long inscripcionId) throws Exception {
        return mvc.perform(post(URL + "/{id}/baja", inscripcionId).with(estudianteLogueado(personaId)));
    }

    private static Long idDe(ResultActions alta) throws Exception {
        String json = alta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private Long insertarMateria(String nombre) {
        Long materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES (?, 4, 64)", nombre);
        insertar("INSERT INTO plan_materia (plan_estudio_id, materia_id, anio_cursado, periodo_lectivo) "
                + "VALUES (?, ?, 1, 'PRIMER_CUATRIMESTRE')", plan, materia);
        return materia;
    }

    private Long insertarEstudiante(String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', 'Díaz', ?, '2233333333', '2005-01-01', ?, ?)
                """, dni, dni + "@finales.test", legajo);
    }

    // examen_final y examen_final_docente son del módulo examenes.
    private Long insertarMesa(Long materia, LocalDate fecha, String horaInicio) {
        Long mesa = insertar("INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id) "
                + "VALUES (?, ?, ?, ?, ADDTIME(?, '02:00'), ?)", materia, turno, fecha, horaInicio, horaInicio, aula);
        jdbc.update("INSERT INTO examen_final_docente (examen_final_id, docente_id) VALUES (?, ?)", mesa, docente);
        return mesa;
    }

    /** Devuelve la inscripción a comisión de la cursada regular. */
    private Long regularizar(Long personaId, Long materia, LocalDate vencimiento) {
        return cerrarCursada(personaId, materia, "REGULAR", 6, vencimiento);
    }

    /** El estudiante cursó la materia el año pasado y el acta quedó en esa condición. */
    private Long cerrarCursada(Long personaId, Long materia, String condicion, Integer nota, LocalDate vencimiento) {
        Long comision = insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, ?, 'MANANA', ?)", plan, ++nroComision, ANIO - 1);
        Long comisionMateria = insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo) "
                + "VALUES (?, ?, 'PRIMER_CUATRIMESTRE')", comision, materia);
        Long inscripcionCarreraDeLaPersona = jdbc.queryForObject(
                "SELECT id FROM inscripcion_carrera WHERE estudiante_id = ? AND plan_estudio_id = ?",
                Long.class, personaId, plan);
        Long cursada = insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, "
                + "comision_materia_id, periodo_id) VALUES (?, ?, ?)",
                inscripcionCarreraDeLaPersona, comisionMateria, periodoCursadaAnterior);
        // cierre_cursada es del módulo examenes.
        insertar("INSERT INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, nota_cursada, "
                + "fecha_cierre, fecha_vencimiento) VALUES (?, ?, ?, ?, ?, ?)",
                cursada, docente, condicion, nota, LocalDate.now().minusMonths(6), vencimiento);
        return cursada;
    }

    // correlatividad_materias es del módulo carreras.
    private void exigirParaRendir(Long materia, Long correlativa, String estado) {
        jdbc.update("""
                INSERT INTO correlatividad_materias (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id,
                                                     estado_para_rendir)
                SELECT ?, pm.id, pc.id, ? FROM plan_materia pm, plan_materia pc
                WHERE pm.plan_estudio_id = ? AND pm.materia_id = ? AND pc.plan_estudio_id = ? AND pc.materia_id = ?
                """, plan, estado, plan, materia, plan, correlativa);
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
