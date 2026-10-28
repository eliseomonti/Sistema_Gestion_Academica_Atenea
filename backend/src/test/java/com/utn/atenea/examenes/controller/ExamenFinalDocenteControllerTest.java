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
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las mesas del docente y la carga de notas de final, contra la base real; {@code @Transactional}
 * deshace todo. Lo de otros módulos se inserta con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExamenFinalDocenteControllerTest {

    private static final String URL = "/api/docente/examenes-finales";
    private static final LocalDate JULIO = LocalDate.of(2025, 7, 10);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long carrera;
    Long plan;
    Long algebra;
    Long docente;
    Long otroDocente;
    Long aula;
    Long turno;
    Long mesaDeJulio;          // del tribunal del docente, ya se tomó
    Long inscripcionAcosta;
    Long inscripcionZapata;
    Long inscripcionMedina;    // se dio de baja de la mesa

    @BeforeEach
    void crearDatos() {
        carrera = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba mesas docente', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carrera);
        algebra = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Álgebra de prueba mesas docente', 4, 64)");
        docente = insertarDocente("Ana", "Ruiz", "30001001", 981001L);
        otroDocente = insertarDocente("Luis", "Paz", "30001002", 981002L);
        aula = insertar("INSERT INTO aula (nombre) VALUES ('Aula prueba mesas docente')");
        turno = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', 2025, '2025-07-01', '2025-07-05')
                """, carrera);

        mesaDeJulio = insertarMesa(JULIO, "09:00", aula, docente);
        inscripcionZapata = inscribirAlFinal("Zapata", "40001001", 981011L, mesaDeJulio);
        inscripcionAcosta = inscribirAlFinal("Acosta", "40001002", 981012L, mesaDeJulio);
        inscripcionMedina = inscribirAlFinal("Medina", "40001003", 981013L, mesaDeJulio);
        jdbc.update("UPDATE estudiante_inscripcion_examen_final SET estado = 'BAJA', fecha_baja = fecha_inscripcion "
                + "WHERE id = ?", inscripcionMedina);
    }

    // --- Mis mesas ---

    @Test
    void listarDevuelveLasMesasDelAnioEnCuyoTribunalEsta() throws Exception {
        Long mesaDeDiciembre = insertarMesa(LocalDate.of(2025, 12, 1), "14:00", null, docente);
        insertarMesa(LocalDate.of(2025, 8, 1), "09:00", null, otroDocente);
        insertarMesa(LocalDate.of(2024, 12, 1), "09:00", null, docente);

        mvc.perform(get(URL).with(docenteLogueado(docente)).param("anio", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].examenFinalId").value(contains(mesaDeJulio.intValue(), mesaDeDiciembre.intValue())))
                .andExpect(jsonPath("$[0].nombreMateria").value("Álgebra de prueba mesas docente"))
                .andExpect(jsonPath("$[0].fecha").value("2025-07-10"))
                .andExpect(jsonPath("$[0].nombreAula").value("Aula prueba mesas docente"))
                .andExpect(jsonPath("$[1].nombreAula").doesNotExist());
    }

    // --- Notas ---

    @Test
    void cargarNotasDevuelveLaPlanillaDeLaMesa() throws Exception {
        cargarNotas(mesaDeJulio, docente, nota(inscripcionZapata, "PRESENTE", 4), nota(inscripcionAcosta, "PRESENTE", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examenFinalId").value(mesaDeJulio))
                .andExpect(jsonPath("$.nombreMateria").value("Álgebra de prueba mesas docente"))
                .andExpect(jsonPath("$.notas[*].apellido").value(contains("Acosta", "Zapata")))
                .andExpect(jsonPath("$.notas[0].nota").value(7))
                .andExpect(jsonPath("$.notas[1].nota").value(4));
        mvc.perform(get(URL + "/{id}/notas", mesaDeJulio).with(docenteLogueado(docente)))
                .andExpect(jsonPath("$.notas[*].nota").value(contains(7, 4)));
    }

    @Test
    void corregirUnaNotaDeFinalAgregaUnaRectificacion() throws Exception {
        cargarNotas(mesaDeJulio, docente, nota(inscripcionAcosta, "PRESENTE", 5)).andExpect(status().isOk());

        cargarNotas(mesaDeJulio, docente, nota(inscripcionAcosta, "PRESENTE", 6))
                .andExpect(jsonPath("$.notas[0].nota").value(6));
        Integer filas = jdbc.queryForObject("SELECT COUNT(*) FROM nota_final WHERE inscripcion_examen_final_id = ?",
                Integer.class, inscripcionAcosta);
        assertThat(filas).isEqualTo(2);
    }

    @Test
    void sinNotasLaPlanillaTraeATodosLosAnotadosSinNota() throws Exception {
        mvc.perform(get(URL + "/{id}/notas", mesaDeJulio).with(docenteLogueado(docente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas[*].inscripcionId").value(
                        contains(inscripcionAcosta.intValue(), inscripcionZapata.intValue())))
                .andExpect(jsonPath("$.notas[0].condicion").doesNotExist());
    }

    @Test
    void noSeCarganNotasDeUnaMesaQueTodaviaNoSeTomo() throws Exception {
        LocalDate dentroDeUnaSemana = LocalDate.now().plusDays(7);
        Long mesaQueViene = insertarMesa(dentroDeUnaSemana, "09:00", null, docente);

        cargarNotas(mesaQueViene, docente, nota(inscripcionAcosta, "PRESENTE", 7))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La mesa todavía no se tomó: es el "
                        + dentroDeUnaSemana.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    @Test
    void noSeCarganNotasDeQuienSeDioDeBaja() throws Exception {
        cargarNotas(mesaDeJulio, docente, nota(inscripcionMedina, "PRESENTE", 7))
                .andExpect(status().isConflict());
    }

    @Test
    void unDocenteQueNoEsDelTribunalRecibe403() throws Exception {
        mvc.perform(get(URL + "/{id}/notas", mesaDeJulio).with(docenteLogueado(otroDocente)))
                .andExpect(status().isForbidden());
        cargarNotas(mesaDeJulio, otroDocente, nota(inscripcionAcosta, "PRESENTE", 7))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaMesaInexistenteDevuelve404() throws Exception {
        mvc.perform(get(URL + "/{id}/notas", -1L).with(docenteLogueado(docente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la mesa de examen con id -1"));
    }

    // --- Ayudas ---

    private ResultActions cargarNotas(Long mesaId, Long docenteId, String... notas) throws Exception {
        return mvc.perform(put(URL + "/{id}/notas", mesaId).with(docenteLogueado(docenteId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notas\": [" + String.join(", ", notas) + "]}"));
    }

    private static String nota(Long inscripcionId, String condicion, Integer nota) {
        return "{\"inscripcionId\": %d, \"condicion\": \"%s\", \"nota\": %s}".formatted(inscripcionId, condicion, nota);
    }

    private Long insertarMesa(LocalDate fecha, String horaInicio, Long aulaId, Long docenteDelTribunal) {
        Long mesa = insertar("INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id) "
                + "VALUES (?, ?, ?, ?, ADDTIME(?, '02:00'), ?)", algebra, turno, fecha, horaInicio, horaInicio, aulaId);
        jdbc.update("INSERT INTO examen_final_docente (examen_final_id, docente_id) VALUES (?, ?)",
                mesa, docenteDelTribunal);
        return mesa;
    }

    private Long insertarDocente(String nombre, String apellido, String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES (?, ?, ?, '2231111111', '1980-01-01', ?, ?)
                """, nombre, apellido, dni, dni + "@mesas-docente.test", legajo);
    }

    // persona e inscripcion_carrera son de personas; estudiante_inscripcion_examen_final, de inscripciones.
    private Long inscribirAlFinal(String apellido, String dni, Long legajo, Long mesa) {
        Long estudiante = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', ?, ?, '2233333333', '2005-01-01', ?, ?)
                """, apellido, dni, dni + "@mesas-docente.test", legajo);
        Long inscripcionCarrera = insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) "
                + "VALUES (?, ?)", estudiante, plan);
        return insertar("INSERT INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id) "
                + "VALUES (?, ?)", inscripcionCarrera, mesa);
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
