package com.utn.atenea.examenes.controller;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El acta de cierre de cursada que carga el docente, contra la base real; {@code @Transactional} deshace
 * todo. Lo de otros módulos se inserta con SQL. La vigencia de la regularidad es la de
 * application.properties: 2 años.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CierreCursadaDocenteControllerTest {

    private static final int ANIO = LocalDate.now().getYear();
    private static final LocalDate HOY = LocalDate.now();

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long plan;
    Long docente;
    Long otroDocente;
    Long algebra;              // materia de la comisión que dicta el docente
    Long inscripcionAcosta;
    Long inscripcionZapata;
    Long inscripcionMedina;    // se dio de baja de la cursada

    @BeforeEach
    void crearDatos() {
        Long carrera = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba acta', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carrera);
        Long materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Álgebra de prueba acta', 4, 64)");
        docente = insertarDocente("Ana", "Ruiz", "30001101", 981101L);
        otroDocente = insertarDocente("Luis", "Paz", "30001102", 981102L);

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
        inscripcionZapata = inscribir("Zapata", "40001101", 981111L, periodo);
        inscripcionAcosta = inscribir("Acosta", "40001102", 981112L, periodo);
        inscripcionMedina = inscribir("Medina", "40001103", 981113L, periodo);
        jdbc.update("UPDATE estudiante_inscripcion_comision SET estado = 'BAJA', fecha_baja = CURRENT_DATE WHERE id = ?",
                inscripcionMedina);
    }

    @Test
    void cerrarDejaACadaUnoConSuCondicionYSoloLaRegularidadVence() throws Exception {
        cerrar(docente, cierre(inscripcionAcosta, "PROMOCIONADO", 9), cierre(inscripcionZapata, "REGULAR", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comisionMateriaId").value(algebra))
                .andExpect(jsonPath("$.estudiantes[*].apellido").value(contains("Acosta", "Zapata")))
                .andExpect(jsonPath("$.estudiantes[0].condicion").value("PROMOCIONADO"))
                .andExpect(jsonPath("$.estudiantes[0].nota").value(9))
                .andExpect(jsonPath("$.estudiantes[0].fechaCierre").value(HOY.toString()))
                .andExpect(jsonPath("$.estudiantes[0].fechaVencimiento").doesNotExist())
                .andExpect(jsonPath("$.estudiantes[1].condicion").value("REGULAR"))
                .andExpect(jsonPath("$.estudiantes[1].fechaVencimiento").value(HOY.plusYears(2).toString()));
        mvc.perform(get(acta()).with(docenteLogueado(docente)))
                .andExpect(jsonPath("$.estudiantes[*].condicion").value(contains("PROMOCIONADO", "REGULAR")));
    }

    @Test
    void sinActaTraeATodosLosAnotadosSinCondicion() throws Exception {
        mvc.perform(get(acta()).with(docenteLogueado(docente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiantes[*].inscripcionComisionId").value(
                        contains(inscripcionAcosta.intValue(), inscripcionZapata.intValue())))
                .andExpect(jsonPath("$.estudiantes[0].condicion").doesNotExist())
                .andExpect(jsonPath("$.estudiantes[0].fechaCierre").doesNotExist());
    }

    @Test
    void corregirElActaConservaLaFechaDeCierreOriginal() throws Exception {
        LocalDate haceUnMes = HOY.minusMonths(1);
        // cierre_cursada es append-only: el acta original se inserta, y la corrección agrega otra.
        insertar("INSERT INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, nota_cursada, "
                + "fecha_cierre, fecha_vencimiento) VALUES (?, ?, 'REGULAR', 6, ?, ?)",
                inscripcionZapata, docente, haceUnMes, haceUnMes.plusYears(2));

        cerrar(docente, cierre(inscripcionZapata, "NO_REGULAR", 3))
                .andExpect(jsonPath("$.estudiantes[1].condicion").value("NO_REGULAR"))
                .andExpect(jsonPath("$.estudiantes[1].fechaCierre").value(haceUnMes.toString()))
                .andExpect(jsonPath("$.estudiantes[1].fechaVencimiento").doesNotExist());
        // Y si vuelve a regular, vence a los 2 años del cierre original, no de la corrección.
        cerrar(docente, cierre(inscripcionZapata, "REGULAR", 5))
                .andExpect(jsonPath("$.estudiantes[1].fechaVencimiento").value(haceUnMes.plusYears(2).toString()));
        assertFilasDeActa(inscripcionZapata, 3);
    }

    @Test
    void cerrarConLaMismaCondicionYNotaNoHaceNada() throws Exception {
        cerrar(docente, cierre(inscripcionAcosta, "REGULAR", 7)).andExpect(status().isOk());
        cerrar(docente, cierre(inscripcionAcosta, "REGULAR", 7)).andExpect(status().isOk());

        assertFilasDeActa(inscripcionAcosta, 1);
    }

    @Test
    void laPromocionSinNotaDevuelve400() throws Exception {
        cerrar(docente, cierre(inscripcionAcosta, "PROMOCIONADO", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("cierres[0].notaSiPromociona"));
    }

    @Test
    void noSeCierraLaCursadaDeQuienSeDioDeBajaNiRepetida() throws Exception {
        cerrar(docente, cierre(inscripcionMedina, "REGULAR", 6))
                .andExpect(status().isConflict());
        cerrar(docente, cierre(inscripcionAcosta, "REGULAR", 6), cierre(inscripcionAcosta, "NO_REGULAR", 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La inscripción " + inscripcionAcosta + " está más de una vez"));
    }

    @Test
    void unDocenteQueNoDictaLaMateriaNoCierraLaCursada() throws Exception {
        cerrar(otroDocente, cierre(inscripcionAcosta, "REGULAR", 6)).andExpect(status().isForbidden());
        mvc.perform(get(acta()).with(docenteLogueado(otroDocente))).andExpect(status().isForbidden());
    }

    @Test
    void conActaYaNoSeCarganNotasDeLaCursada() throws Exception {
        Long parcial = insertar("INSERT INTO evaluacion_cursada (comision_materia_id, nombre, fecha) "
                + "VALUES (?, 'Primer parcial', ?)", algebra, HOY.minusDays(1));
        cerrar(docente, cierre(inscripcionAcosta, "REGULAR", 6)).andExpect(status().isOk());

        cargarNota(parcial, inscripcionAcosta)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La cursada de la inscripción " + inscripcionAcosta
                        + " ya tiene acta de cierre: sus notas no se cambian"));
        cargarNota(parcial, inscripcionZapata).andExpect(status().isOk());
    }

    // --- Ayudas ---

    private String acta() {
        return "/api/docente/comisiones-materia/" + algebra + "/cierre";
    }

    private ResultActions cerrar(Long docenteId, String... cierres) throws Exception {
        return mvc.perform(put(acta()).with(docenteLogueado(docenteId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cierres\": [" + String.join(", ", cierres) + "]}"));
    }

    private static String cierre(Long inscripcionComisionId, String condicion, Integer nota) {
        return "{\"inscripcionComisionId\": %d, \"condicion\": \"%s\", \"nota\": %s}"
                .formatted(inscripcionComisionId, condicion, nota);
    }

    private ResultActions cargarNota(Long evaluacionId, Long inscripcionId) throws Exception {
        return mvc.perform(put("/api/docente/evaluaciones/{id}/notas", evaluacionId).with(docenteLogueado(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notas\": [{\"inscripcionId\": %d, \"condicion\": \"PRESENTE\", \"nota\": 8}]}"
                        .formatted(inscripcionId)));
    }

    private void assertFilasDeActa(Long inscripcionComisionId, int esperadas) {
        Integer filas = jdbc.queryForObject("SELECT COUNT(*) FROM cierre_cursada WHERE inscripcion_comision_id = ?",
                Integer.class, inscripcionComisionId);
        assertThat(filas).isEqualTo(esperadas);
    }

    private Long insertarDocente(String nombre, String apellido, String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES (?, ?, ?, '2231111111', '1980-01-01', ?, ?)
                """, nombre, apellido, dni, dni + "@acta.test", legajo);
    }

    // persona e inscripcion_carrera son de personas; estudiante_inscripcion_comision, de inscripciones.
    private Long inscribir(String apellido, String dni, Long legajo, Long periodo) {
        Long estudiante = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', ?, ?, '2233333333', '2005-01-01', ?, ?)
                """, apellido, dni, dni + "@acta.test", legajo);
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
