package com.utn.atenea.comisiones.controller;

import com.jayway.jsonpath.JsonPath;
import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.model.Aula;
import com.utn.atenea.comisiones.model.Comision;
import com.utn.atenea.comisiones.model.ComisionMateria;
import com.utn.atenea.comisiones.model.DiaSemana;
import com.utn.atenea.comisiones.model.RolDocente;
import com.utn.atenea.comisiones.model.Turno;
import com.utn.atenea.comisiones.repository.AulaRepository;
import com.utn.atenea.comisiones.repository.ComisionRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ABM del grupo, listado y detalle contra la base real; {@code @Transactional} deshace todo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComisionControllerTest {

    private static final String URL = "/api/admin/comisiones";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ComisionRepository comisionRepository;
    @Autowired AulaRepository aulaRepository;

    Long carreraId;
    Long plan;
    Long otroPlanDeLaCarrera;
    Long planSinMaterias;
    Long planDeOtraCarrera;
    Long programacion;   // 1° año, 2° cuatrimestre
    Long ingles;         // 1° año, 1° cuatrimestre
    Long analisis;       // 2° año, anual

    @BeforeEach
    void crearPlanesConMaterias() {
        // carrera, plan, materia y plan_materia son del módulo carreras: se insertan con SQL.
        carreraId = insertarCarrera("Carrera de prueba comisiones");
        plan = insertarPlan(carreraId, "Plan 2024");
        programacion = insertarMateria(plan, "Programación II de prueba", 1, PeriodoLectivo.SEGUNDO_CUATRIMESTRE);
        ingles = insertarMateria(plan, "Inglés I de prueba", 1, PeriodoLectivo.PRIMER_CUATRIMESTRE);
        analisis = insertarMateria(plan, "Análisis de prueba", 2, PeriodoLectivo.ANUAL);

        otroPlanDeLaCarrera = insertarPlan(carreraId, "Plan 2003");
        insertarMateria(otroPlanDeLaCarrera, "Materia del plan viejo", 1, PeriodoLectivo.ANUAL);
        planSinMaterias = insertarPlan(carreraId, "Plan 2025");

        planDeOtraCarrera = insertarPlan(insertarCarrera("Otra carrera de prueba comisiones"), "Plan 2024");
        insertarMateria(planDeOtraCarrera, "Materia de otra carrera", 1, PeriodoLectivo.ANUAL);
    }

    // --- Alta ---

    @Test
    void altaCreaLaComisionConSuNombreDePlanilla() throws Exception {
        alta(plan, 2, 1, "MANANA", 2026)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("1/25"))
                .andExpect(jsonPath("$.planEstudioId").value(plan))
                .andExpect(jsonPath("$.anioCursado").value(2))
                .andExpect(jsonPath("$.turno").value("MANANA"))
                .andExpect(jsonPath("$.cantidadMaterias").value(0))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void elMismoNumeroSePuedeUsarEnOtroAnioDeLaCarrera() throws Exception {
        alta(plan, 1, 1, "MANANA", 2026).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("1/26"));
        alta(plan, 2, 1, "MANANA", 2026).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("1/25"));
    }

    @Test
    void numeroRepetidoEnElMismoAnioDevuelve409() throws Exception {
        alta(plan, 1, 1, "MANANA", 2026).andExpect(status().isCreated());

        alta(plan, 1, 1, "NOCHE", 2026)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Ya existe la comisión 1 de 1° año en el plan de estudio " + plan + " para 2026"));
    }

    @Test
    void planInexistenteDevuelve404() throws Exception {
        alta(-1L, 1, 1, "MANANA", 2026)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el plan de estudio con id -1"));
    }

    @Test
    void anioDeLaCarreraQueElPlanNoTieneDevuelve409() throws Exception {
        alta(plan, 3, 1, "MANANA", 2026)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El plan de estudio " + plan + " tiene 2 años: no tiene materias de 3° año"));
    }

    @Test
    void planSinMateriasDevuelve409() throws Exception {
        alta(planSinMaterias, 1, 1, "MANANA", 2026)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El plan de estudio " + planSinMaterias + " no tiene materias cargadas"));
    }

    @Test
    void requestSinTurnoDevuelve400ConElCampo() throws Exception {
        mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"planEstudioId": %d, "anioCursado": 1, "nroComision": 1, "anioLectivo": 2026}
                        """.formatted(plan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("turno"));
    }

    // --- Modificación ---

    @Test
    void modificarCambiaNumeroYTurno() throws Exception {
        long id = altaId(plan, 1, 1, "MANANA", 2026);

        modificar(id, 7, "NOCHE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("7/26"))
                .andExpect(jsonPath("$.turno").value("NOCHE"));
    }

    @Test
    void modificarConservandoSuPropioNumeroNoChocaConsigoMisma() throws Exception {
        long id = altaId(plan, 1, 1, "MANANA", 2026);

        modificar(id, 1, "TARDE").andExpect(status().isOk());
    }

    @Test
    void modificarAlNumeroDeOtraComisionDevuelve409() throws Exception {
        altaId(plan, 1, 1, "MANANA", 2026);
        long otra = altaId(plan, 1, 2, "TARDE", 2026);

        modificar(otra, 1, "TARDE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void unaComisionDadaDeBajaNoSePuedeModificar() throws Exception {
        long id = altaId(plan, 1, 1, "MANANA", 2026);
        mvc.perform(post(URL + "/{id}/baja", id).with(admin())).andExpect(status().isOk());

        modificar(id, 2, "MANANA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La comisión " + id + " está dada de baja"));
    }

    // --- Baja y borrado ---

    @Test
    void bajaLaDejaInactivaYNoSePuedeRepetir() throws Exception {
        long id = altaId(plan, 1, 1, "MANANA", 2026);

        mvc.perform(post(URL + "/{id}/baja", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
        mvc.perform(get(URL + "/{id}", id).with(admin()))
                .andExpect(jsonPath("$.fechaBaja").value(LocalDate.now().toString()));

        mvc.perform(post(URL + "/{id}/baja", id).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La comisión " + id + " ya está dada de baja"));
    }

    @Test
    void eliminarBorraLaComisionConSusMaterias() throws Exception {
        Comision comision = new Comision(plan, 1, 1, Turno.MANANA, 2026);
        comision.agregarMateria(programacion, PeriodoLectivo.SEGUNDO_CUATRIMESTRE, 40)
                .agregarHorario(DiaSemana.LUNES, LocalTime.of(8, 30), LocalTime.of(10, 30), null);
        comisionRepository.saveAndFlush(comision);

        mvc.perform(delete(URL + "/{id}", comision.getId()).with(admin()))
                .andExpect(status().isNoContent());

        assertThat(comisionRepository.existsById(comision.getId())).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comision_materia WHERE comision_id = ?",
                Integer.class, comision.getId())).isZero();
    }

    @Test
    void eliminarUnaComisionConHistoriaDevuelve409() throws Exception {
        Comision comision = new Comision(plan, 1, 1, Turno.MANANA, 2026);
        ComisionMateria materia = comision.agregarMateria(programacion, PeriodoLectivo.SEGUNDO_CUATRIMESTRE, 40);
        comisionRepository.saveAndFlush(comision);
        // Una encuesta es lo más simple que cuelga de una materia de la comisión.
        jdbc.update("""
                INSERT INTO encuesta (comision_materia_id, fecha_apertura, fecha_cierre)
                VALUES (?, '2026-11-01 00:00:00', '2026-11-30 00:00:00')
                """, materia.getId());

        mvc.perform(delete(URL + "/{id}", comision.getId()).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La comisión " + comision.getId()
                        + " tiene inscripciones, evaluaciones o encuestas: no se puede borrar, sólo dar de baja"));
    }

    @Test
    void eliminarUnaComisionInexistenteDevuelve404() throws Exception {
        mvc.perform(delete(URL + "/{id}", -1).with(admin()))
                .andExpect(status().isNotFound());
    }

    // --- Listado ---

    @Test
    void listaLasComisionesDeLaCarreraOrdenadasPorAnioYNumero() throws Exception {
        altaId(plan, 2, 1, "MANANA", 2026);
        altaId(plan, 1, 2, "TARDE", 2026);
        altaId(plan, 1, 1, "MANANA", 2026);
        altaId(otroPlanDeLaCarrera, 1, 5, "NOCHE", 2026);
        altaId(planDeOtraCarrera, 1, 1, "MANANA", 2026);
        altaId(plan, 1, 3, "MANANA", 2025);

        listar("carreraId", carreraId, "anioLectivo", 2026)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", contains("1/26", "2/26", "5/26", "1/25")));
    }

    @Test
    void filtraPorPlanAnioDeLaCarreraYTurno() throws Exception {
        altaId(plan, 1, 1, "MANANA", 2026);
        altaId(plan, 1, 2, "TARDE", 2026);
        altaId(plan, 2, 1, "MANANA", 2026);
        altaId(otroPlanDeLaCarrera, 1, 5, "MANANA", 2026);

        listar("carreraId", carreraId, "anioLectivo", 2026, "planEstudioId", plan, "anioCursado", 1, "turno", "MANANA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", contains("1/26")));
    }

    @Test
    void filtraPorPeriodoSegunLasMateriasQueTiene() throws Exception {
        Comision conProgramacion = new Comision(plan, 1, 1, Turno.MANANA, 2026);
        conProgramacion.agregarMateria(programacion, PeriodoLectivo.SEGUNDO_CUATRIMESTRE, null);
        comisionRepository.save(conProgramacion);
        Comision conIngles = new Comision(plan, 1, 2, Turno.MANANA, 2026);
        conIngles.agregarMateria(ingles, PeriodoLectivo.PRIMER_CUATRIMESTRE, null);
        comisionRepository.save(conIngles);

        listar("carreraId", carreraId, "anioLectivo", 2026, "periodoLectivo", "SEGUNDO_CUATRIMESTRE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", contains("1/26")))
                .andExpect(jsonPath("$[0].cantidadMaterias").value(1));
    }

    @Test
    void sinAnioLectivoTomaElActualYSinActivaLasActivas() throws Exception {
        int actual = LocalDate.now().getYear();
        altaId(plan, 1, 1, "MANANA", actual);
        long dadaDeBaja = altaId(plan, 1, 2, "MANANA", actual);
        mvc.perform(post(URL + "/{id}/baja", dadaDeBaja).with(admin())).andExpect(status().isOk());
        altaId(plan, 1, 3, "MANANA", actual - 1);

        listar("carreraId", carreraId)
                .andExpect(jsonPath("$[*].nroComision", contains(1)));
        listar("carreraId", carreraId, "activa", false)
                .andExpect(jsonPath("$[*].nroComision", contains(2)));
    }

    @Test
    void planDeOtraCarreraDevuelve404() throws Exception {
        listar("carreraId", carreraId, "planEstudioId", planDeOtraCarrera)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el plan de estudio con id " + planDeOtraCarrera
                        + " en la carrera con id " + carreraId));
    }

    @Test
    void sinCarreraDevuelve400() throws Exception {
        mvc.perform(get(URL).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("carreraId"));
    }

    // --- Detalle ---

    @Test
    void detalleTraeMateriasConNombreDocentesYHorarios() throws Exception {
        Long titular = insertarDocente("Etcheber", "Esteban", "30000011", 990011L);
        Long ayudante = insertarDocente("Nieto", "Nora", "30000012", 990012L);
        Aula laboratorio = aulaRepository.save(new Aula("Laboratorio prueba comisiones", 30));

        Comision comision = new Comision(plan, 1, 1, Turno.MANANA, 2026);
        ComisionMateria prog = comision.agregarMateria(programacion, PeriodoLectivo.SEGUNDO_CUATRIMESTRE, 40);
        prog.asignarDocente(ayudante, RolDocente.AYUDANTE);
        prog.asignarDocente(titular, RolDocente.TITULAR);
        prog.agregarHorario(DiaSemana.MARTES, LocalTime.of(8, 30), LocalTime.of(10, 30), laboratorio);
        prog.agregarHorario(DiaSemana.LUNES, LocalTime.of(11, 0), LocalTime.of(13, 0), null);
        comision.agregarMateria(ingles, PeriodoLectivo.PRIMER_CUATRIMESTRE, null);
        comisionRepository.saveAndFlush(comision);

        mvc.perform(get(URL + "/{id}", comision.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("1/26"))
                // Primero el 1° cuatrimestre.
                .andExpect(jsonPath("$.materias[*].nombreMateria", contains("Inglés I de prueba", "Programación II de prueba")))
                .andExpect(jsonPath("$.materias[1].cupoMaximo").value(40))
                .andExpect(jsonPath("$.materias[1].periodoLectivo").value("SEGUNDO_CUATRIMESTRE"))
                // El titular primero.
                .andExpect(jsonPath("$.materias[1].docentes[*].apellido", contains("Etcheber", "Nieto")))
                .andExpect(jsonPath("$.materias[1].docentes[0].rol").value("TITULAR"))
                // Por día: el lunes (virtual, sin aula) antes que el martes.
                .andExpect(jsonPath("$.materias[1].horarios[*].diaSemana", contains("LUNES", "MARTES")))
                .andExpect(jsonPath("$.materias[1].horarios[0].aulaId").doesNotExist())
                .andExpect(jsonPath("$.materias[1].horarios[1].horaInicio").value("08:30:00"))
                .andExpect(jsonPath("$.materias[1].horarios[1].aulaNombre").value("Laboratorio prueba comisiones"));
    }

    @Test
    void detalleDeUnaComisionInexistenteDevuelve404() throws Exception {
        mvc.perform(get(URL + "/{id}", -1).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la comisión con id -1"));
    }

    // --- Ayudantes ---

    private ResultActions alta(Long planEstudioId, int anioCursado, int nroComision, String turno, int anioLectivo)
            throws Exception {
        return mvc.perform(post(URL).with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                {"planEstudioId": %d, "anioCursado": %d, "nroComision": %d, "turno": "%s", "anioLectivo": %d}
                """.formatted(planEstudioId, anioCursado, nroComision, turno, anioLectivo)));
    }

    private long altaId(Long planEstudioId, int anioCursado, int nroComision, String turno, int anioLectivo)
            throws Exception {
        String respuesta = alta(planEstudioId, anioCursado, nroComision, turno, anioLectivo)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(respuesta, "$.id").longValue();
    }

    private ResultActions modificar(long comisionId, int nroComision, String turno) throws Exception {
        return mvc.perform(put(URL + "/{id}", comisionId).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nroComision": %d, "turno": "%s"}
                        """.formatted(nroComision, turno)));
    }

    private ResultActions listar(Object... parametros) throws Exception {
        var request = get(URL).with(admin());
        for (int i = 0; i < parametros.length; i += 2) {
            request.param((String) parametros[i], String.valueOf(parametros[i + 1]));
        }
        return mvc.perform(request);
    }

    private Long insertarCarrera(String nombre) {
        jdbc.update("""
                INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera)
                VALUES (?, '2 años', 'Técnico', 'PRESENCIAL')
                """, nombre);
        return ultimoId();
    }

    private Long insertarPlan(Long carreraId, String nombre) {
        jdbc.update("INSERT INTO plan_estudio (carrera_id, nombre, anio_inicio) VALUES (?, ?, 2024)", carreraId, nombre);
        return ultimoId();
    }

    private Long insertarMateria(Long planEstudioId, String nombre, int anioCursado, PeriodoLectivo periodo) {
        jdbc.update("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) VALUES (?, 4, 64)", nombre);
        Long materiaId = ultimoId();
        jdbc.update("""
                INSERT INTO plan_materia (plan_estudio_id, materia_id, anio_cursado, periodo_lectivo)
                VALUES (?, ?, ?, ?)
                """, planEstudioId, materiaId, anioCursado, periodo.name());
        return materiaId;
    }

    // persona es del módulo personas: también con SQL.
    private Long insertarDocente(String apellido, String nombre, String dni, Long legajo) {
        jdbc.update("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES (?, ?, ?, '2231234567', '1980-01-01', ?, ?)
                """, nombre, apellido, dni, dni + "@atenea.test", legajo);
        return ultimoId();
    }

    private Long ultimoId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
