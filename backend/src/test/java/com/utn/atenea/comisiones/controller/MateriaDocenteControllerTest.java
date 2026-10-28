package com.utn.atenea.comisiones.controller;

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
 * Las materias de comisión que dicta el docente, contra la base real; {@code @Transactional} deshace
 * todo. Lo de otros módulos (carreras, personas) se inserta con SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MateriaDocenteControllerTest {

    private static final String URL = "/api/docente/comisiones-materia";
    private static final int ANIO = LocalDate.now().getYear();

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    Long plan;
    Long algebra;
    Long analisis;
    Long docente;
    Long otroDocente;
    Long algebraEsteAnio;
    Long algebraElAnioPasado;

    @BeforeEach
    void crearDatos() {
        Long carrera = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba materias docente', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carrera);
        algebra = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Álgebra de prueba materias docente', 4, 64)");
        analisis = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Análisis de prueba materias docente', 4, 64)");
        docente = insertarDocente("Ana", "Ruiz", "30000801", 980801L);
        otroDocente = insertarDocente("Luis", "Paz", "30000802", 980802L);

        Long comision = insertarComision(1, ANIO);
        algebraEsteAnio = insertarComisionMateria(comision, algebra, docente, "TITULAR");
        insertarComisionMateria(comision, analisis, otroDocente, "TITULAR");
        Long aula = insertar("INSERT INTO aula (nombre) VALUES ('Aula prueba materias docente')");
        insertar("INSERT INTO comision_horario (comision_materia_id, dia_semana, hora_inicio, hora_fin, aula_id) "
                + "VALUES (?, 'LUNES', '08:00', '10:00', ?)", algebraEsteAnio, aula);
        algebraElAnioPasado = insertarComisionMateria(insertarComision(1, ANIO - 1), algebra, docente, "AYUDANTE");

        Long dadaDeBaja = insertarComision(2, ANIO);
        jdbc.update("UPDATE comision SET activa = FALSE, fecha_baja = CURRENT_DATE WHERE id = ?", dadaDeBaja);
        insertarComisionMateria(dadaDeBaja, analisis, docente, "TITULAR");
    }

    @Test
    void devuelveLasMateriasQueDictaEnComisionesActivasDelAnio() throws Exception {
        mvc.perform(get(URL).with(docenteLogueado(docente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].comisionMateriaId").value(contains(algebraEsteAnio.intValue())))
                .andExpect(jsonPath("$[0].nombreMateria").value("Álgebra de prueba materias docente"))
                .andExpect(jsonPath("$[0].nombreComision").value("1/" + String.format("%02d", ANIO % 100)))
                .andExpect(jsonPath("$[0].turno").value("MANANA"))
                .andExpect(jsonPath("$[0].anioLectivo").value(ANIO))
                .andExpect(jsonPath("$[0].periodoLectivo").value("PRIMER_CUATRIMESTRE"))
                .andExpect(jsonPath("$[0].rol").value("TITULAR"))
                .andExpect(jsonPath("$[0].horarios[0].diaSemana").value("LUNES"))
                .andExpect(jsonPath("$[0].horarios[0].aulaNombre").value("Aula prueba materias docente"));
    }

    @Test
    void conAnioLectivoDevuelveLasDeEseAnio() throws Exception {
        mvc.perform(get(URL).with(docenteLogueado(docente)).param("anioLectivo", String.valueOf(ANIO - 1)))
                .andExpect(jsonPath("$[*].comisionMateriaId").value(contains(algebraElAnioPasado.intValue())))
                .andExpect(jsonPath("$[0].rol").value("AYUDANTE"));
    }

    @Test
    void unEstudianteNoUsaElAutoservicioDelDocente() throws Exception {
        mvc.perform(get(URL).with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        new UsuarioPrincipal(1L, docente, "estudiante"), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))))
                .andExpect(status().isForbidden());
    }

    // --- Ayudas ---

    private Long insertarDocente(String nombre, String apellido, String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES (?, ?, ?, '2231111111', '1980-01-01', ?, ?)
                """, nombre, apellido, dni, dni + "@materias-docente.test", legajo);
    }

    private Long insertarComision(int nroComision, int anioLectivo) {
        return insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, ?, 'MANANA', ?)", plan, nroComision, anioLectivo);
    }

    private Long insertarComisionMateria(Long comisionId, Long materiaId, Long docenteId, String rol) {
        Long comisionMateria = insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo) "
                + "VALUES (?, ?, 'PRIMER_CUATRIMESTRE')", comisionId, materiaId);
        jdbc.update("INSERT INTO comision_docente (comision_materia_id, docente_id, rol) VALUES (?, ?, ?)",
                comisionMateria, docenteId, rol);
        return comisionMateria;
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
