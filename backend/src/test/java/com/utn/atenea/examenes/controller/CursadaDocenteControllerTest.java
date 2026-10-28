package com.utn.atenea.examenes.controller;

import com.jayway.jsonpath.JsonPath;
import com.utn.atenea.seguridad.UsuarioPrincipal;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lo que el docente hace en una materia que dicta: ver sus estudiantes, armar las evaluaciones y cargar
 * las notas. Contra la base real; {@code @Transactional} deshace todo. Lo de otros módulos se inserta con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CursadaDocenteControllerTest {

    private static final int ANIO = LocalDate.now().getYear();
    private static final LocalDate HOY = LocalDate.now();

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long plan;
    Long docente;
    Long otroDocente;
    Long algebra;              // materia de la comisión que dicta el docente
    Long acosta;
    Long zapata;
    Long medina;               // se dio de baja de la cursada
    Long inscripcionAcosta;
    Long inscripcionZapata;
    Long inscripcionMedina;

    @BeforeEach
    void crearDatos() {
        Long carrera = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba cursada docente', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carrera);
        Long materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Álgebra de prueba cursada docente', 4, 64)");
        docente = insertarDocente("Ana", "Ruiz", "30000901", 980901L);
        otroDocente = insertarDocente("Luis", "Paz", "30000902", 980902L);

        Long comision = insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, 1, 'MANANA', ?)", plan, ANIO);
        algebra = insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo) "
                + "VALUES (?, ?, 'PRIMER_CUATRIMESTRE')", comision, materia);
        jdbc.update("INSERT INTO comision_docente (comision_materia_id, docente_id, rol) VALUES (?, ?, 'TITULAR')",
                algebra, docente);

        Long periodo = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', ?, 'PRIMER_CUATRIMESTRE', ?, ?)
                """, carrera, ANIO, LocalDateTime.now().minusMonths(4), LocalDateTime.now().minusMonths(3));
        zapata = insertarEstudiante("Zapata", "40000901", 980911L);
        acosta = insertarEstudiante("Acosta", "40000902", 980912L);
        medina = insertarEstudiante("Medina", "40000903", 980913L);
        inscripcionZapata = inscribir(zapata, periodo);
        inscripcionAcosta = inscribir(acosta, periodo);
        inscripcionMedina = inscribir(medina, periodo);
        jdbc.update("UPDATE estudiante_inscripcion_comision SET estado = 'BAJA', fecha_baja = CURRENT_DATE WHERE id = ?",
                inscripcionMedina);
    }

    // --- Estudiantes (SCRUM-35) ---

    @Test
    void estudiantesDevuelveLosAnotadosQueNoSeDieronDeBajaPorApellido() throws Exception {
        mvc.perform(get(cursada("/estudiantes")).with(docenteLogueado(docente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].apellido").value(contains("Acosta", "Zapata")))
                .andExpect(jsonPath("$[0].inscripcionComisionId").value(inscripcionAcosta))
                .andExpect(jsonPath("$[0].personaId").value(acosta))
                .andExpect(jsonPath("$[0].legajo").value(980912))
                .andExpect(jsonPath("$[0].email").value("40000902@cursada-docente.test"));
    }

    @Test
    void unDocenteQueNoDictaLaMateriaRecibe403() throws Exception {
        mvc.perform(get(cursada("/estudiantes")).with(docenteLogueado(otroDocente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaMateriaDeComisionInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/docente/comisiones-materia/{id}/estudiantes", -1L).with(docenteLogueado(docente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la materia de comisión con id -1"));
    }

    // --- Evaluaciones ---

    @Test
    void altaDeEvaluacionYDeSuRecuperatorioQueCopiaElTipo() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY.minusDays(20), "PARCIAL", null)
                .andExpect(jsonPath("$.comisionMateriaId").value(algebra))
                .andExpect(jsonPath("$.nombre").value("Primer parcial"))
                .andExpect(jsonPath("$.tipo").value("PARCIAL"))
                .andExpect(jsonPath("$.evaluacionRecuperadaId").doesNotExist()));

        altaEvaluacion("Recuperatorio", HOY.minusDays(5), null, parcial)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PARCIAL"))
                .andExpect(jsonPath("$.evaluacionRecuperadaId").value(parcial));
        mvc.perform(get(cursada("/evaluaciones")).with(docenteLogueado(docente)))
                .andExpect(jsonPath("$[*].nombre").value(contains("Primer parcial", "Recuperatorio")));
    }

    @Test
    void unRecuperatorioNoPuedeSerAntesDeLaQueRecupera() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY.minusDays(20), "PARCIAL", null));

        altaEvaluacion("Recuperatorio", HOY.minusDays(30), null, parcial)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El recuperatorio no puede ser antes de Primer parcial"));
    }

    @Test
    void elTipoVaEnLaOriginalYNoEnElRecuperatorio() throws Exception {
        altaEvaluacion("Primer parcial", HOY, null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("tipoSegunRecuperatorio"));
    }

    @Test
    void dosEvaluacionesConElMismoNombreDevuelve409() throws Exception {
        altaEvaluacion("Primer parcial", HOY, "PARCIAL", null).andExpect(status().isCreated());

        altaEvaluacion("Primer parcial", HOY.plusDays(7), "TRABAJO_PRACTICO", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya hay una evaluación Primer parcial en esta materia de la comisión"));
    }

    @Test
    void modificarCambiaNombreYFecha() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY, "PARCIAL", null));

        mvc.perform(put("/api/docente/evaluaciones/{id}", parcial).with(docenteLogueado(docente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Parcial 1\", \"fecha\": \"%s\"}".formatted(HOY.plusDays(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Parcial 1"))
                .andExpect(jsonPath("$.fecha").value(HOY.plusDays(3).toString()));
    }

    @Test
    void unaEvaluacionSinNotasSeBorraYConNotasNo() throws Exception {
        Long sinNotas = idDe(altaEvaluacion("TP 1", HOY, "TRABAJO_PRACTICO", null));
        Long conNotas = idDe(altaEvaluacion("Primer parcial", HOY, "PARCIAL", null));
        cargarNotas(conNotas, docente, nota(inscripcionAcosta, "PRESENTE", 8)).andExpect(status().isOk());

        mvc.perform(delete("/api/docente/evaluaciones/{id}", sinNotas).with(docenteLogueado(docente)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/docente/evaluaciones/{id}", conNotas).with(docenteLogueado(docente)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "La evaluación " + conNotas + " tiene notas o recuperatorios: no se puede borrar"));
    }

    // --- Notas (SCRUM-33 y 34) ---

    @Test
    void cargarNotasDevuelveLaPlanillaConTodosLosAnotados() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY.minusDays(1), "PARCIAL", null));

        cargarNotas(parcial, docente, nota(inscripcionZapata, "AUSENTE", null), nota(inscripcionAcosta, "PRESENTE", 8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Primer parcial"))
                .andExpect(jsonPath("$.notas[*].apellido").value(contains("Acosta", "Zapata")))
                .andExpect(jsonPath("$.notas[0].inscripcionId").value(inscripcionAcosta))
                .andExpect(jsonPath("$.notas[0].condicion").value("PRESENTE"))
                .andExpect(jsonPath("$.notas[0].nota").value(8))
                .andExpect(jsonPath("$.notas[1].condicion").value("AUSENTE"))
                .andExpect(jsonPath("$.notas[1].nota").doesNotExist());
        mvc.perform(get("/api/docente/evaluaciones/{id}/notas", parcial).with(docenteLogueado(docente)))
                .andExpect(jsonPath("$.notas[0].nota").value(8));
    }

    @Test
    void corregirUnaNotaAgregaUnaRectificacionYCargarLaMismaNoHaceNada() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY.minusDays(1), "PARCIAL", null));
        cargarNotas(parcial, docente, nota(inscripcionAcosta, "PRESENTE", 8), nota(inscripcionZapata, "PRESENTE", 4))
                .andExpect(status().isOk());

        // Sólo viene Acosta: Zapata queda como estaba.
        cargarNotas(parcial, docente, nota(inscripcionAcosta, "PRESENTE", 9))
                .andExpect(jsonPath("$.notas[0].nota").value(9))
                .andExpect(jsonPath("$.notas[1].nota").value(4));
        cargarNotas(parcial, docente, nota(inscripcionAcosta, "PRESENTE", 9)).andExpect(status().isOk());

        // El 8 sigue en la base, rectificado por el 9: queda el historial.
        assertFilasDeNotas(parcial, inscripcionAcosta, 2);
        assertFilasDeNotas(parcial, inscripcionZapata, 1);
    }

    @Test
    void noSeCarganNotasDeUnaEvaluacionQueTodaviaNoSeTomo() throws Exception {
        LocalDate manana = HOY.plusDays(1);
        Long parcial = idDe(altaEvaluacion("Primer parcial", manana, "PARCIAL", null));

        cargarNotas(parcial, docente, nota(inscripcionAcosta, "PRESENTE", 8))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La evaluación todavía no se tomó: es el "
                        + manana.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    @Test
    void noSeCarganNotasDeQuienSeDioDeBajaNiRepetidas() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY, "PARCIAL", null));

        cargarNotas(parcial, docente, nota(inscripcionMedina, "PRESENTE", 8))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La inscripción " + inscripcionMedina
                        + " no es de un estudiante anotado que no se haya dado de baja"));
        cargarNotas(parcial, docente, nota(inscripcionAcosta, "PRESENTE", 8), nota(inscripcionAcosta, "PRESENTE", 9))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La inscripción " + inscripcionAcosta + " está más de una vez"));
    }

    @Test
    void unPresenteSinNotaDevuelve400() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY, "PARCIAL", null));

        cargarNotas(parcial, docente, nota(inscripcionAcosta, "PRESENTE", null))
                .andExpect(status().isBadRequest());
        cargarNotas(parcial, docente, nota(inscripcionAcosta, "PRESENTE", 11))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unDocenteQueNoDictaLaMateriaNoCargaNotas() throws Exception {
        Long parcial = idDe(altaEvaluacion("Primer parcial", HOY, "PARCIAL", null));

        cargarNotas(parcial, otroDocente, nota(inscripcionAcosta, "PRESENTE", 8))
                .andExpect(status().isForbidden());
    }

    // --- Ayudas ---

    private String cursada(String ruta) {
        return "/api/docente/comisiones-materia/" + algebra + ruta;
    }

    private ResultActions altaEvaluacion(String nombre, LocalDate fecha, String tipo, Long recuperaA) throws Exception {
        return mvc.perform(post(cursada("/evaluaciones")).with(docenteLogueado(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"%s\", \"fecha\": \"%s\", \"tipo\": %s, \"evaluacionRecuperadaId\": %s}"
                        .formatted(nombre, fecha, tipo == null ? null : "\"" + tipo + "\"", recuperaA)));
    }

    private ResultActions cargarNotas(Long evaluacionId, Long docenteId, String... notas) throws Exception {
        return mvc.perform(put("/api/docente/evaluaciones/{id}/notas", evaluacionId).with(docenteLogueado(docenteId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notas\": [" + String.join(", ", notas) + "]}"));
    }

    private static String nota(Long inscripcionId, String condicion, Integer nota) {
        return "{\"inscripcionId\": %d, \"condicion\": \"%s\", \"nota\": %s}".formatted(inscripcionId, condicion, nota);
    }

    private void assertFilasDeNotas(Long evaluacionId, Long inscripcionId, int esperadas) {
        Integer filas = jdbc.queryForObject("SELECT COUNT(*) FROM nota_evaluacion "
                + "WHERE evaluacion_cursada_id = ? AND inscripcion_comision_id = ?", Integer.class, evaluacionId, inscripcionId);
        assertThat(filas).isEqualTo(esperadas);
    }

    private static Long idDe(ResultActions alta) throws Exception {
        String json = alta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private Long insertarDocente(String nombre, String apellido, String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES (?, ?, ?, '2231111111', '1980-01-01', ?, ?)
                """, nombre, apellido, dni, dni + "@cursada-docente.test", legajo);
    }

    private Long insertarEstudiante(String apellido, String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', ?, ?, '2233333333', '2005-01-01', ?, ?)
                """, apellido, dni, dni + "@cursada-docente.test", legajo);
    }

    // inscripcion_carrera es de personas y estudiante_inscripcion_comision, de inscripciones.
    private Long inscribir(Long estudiante, Long periodo) {
        Long inscripcionCarrera = insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) "
                + "VALUES (?, ?)", estudiante, plan);
        return insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, comision_materia_id, "
                + "periodo_id) VALUES (?, ?, ?)", inscripcionCarrera, algebra, periodo);
    }

    private Long insertar(String sql, Object... parametros) {
        jdbc.update(sql, parametros);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static RequestPostProcessor docenteLogueado(Long personaId) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new UsuarioPrincipal(1L, personaId, "docente"), null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))));
    }
}
