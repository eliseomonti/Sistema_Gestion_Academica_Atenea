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
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Materias de una comisión, con todos sus controles, contra la base real; {@code @Transactional} deshace todo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComisionMateriaControllerTest {

    private static final String URL = "/api/admin/comisiones/{comisionId}/materias";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ComisionRepository comisionRepository;
    @Autowired AulaRepository aulaRepository;

    Long plan;
    Long programacion;   // 1° año, 2° cuatrimestre
    Long baseDeDatos;    // 1° año, 2° cuatrimestre
    Long ingles;         // 1° año, 1° cuatrimestre
    Long analisis;       // 2° año, anual
    Long deOtroPlan;

    Long titular;
    Long ayudante;
    Long otroDocente;
    Long docenteDadoDeBaja;
    Long estudiante;

    Aula laboratorioA;
    Aula laboratorioB;
    Aula aulaCerrada;

    Comision comision;      // 1° año, 2026: en la que se agregan materias
    Comision otraComision;  // 1° año, 2026: Base de Datos los lunes de 8:30 a 10:30 en el laboratorio A, con otroDocente

    @BeforeEach
    void crearDatos() {
        // carreras y personas son de otros módulos: se insertan con SQL.
        Long carrera = insertarCarrera("Carrera de prueba materias de comisión");
        plan = insertarPlan(carrera, "Plan 2024");
        programacion = insertarMateria(plan, "Programación II de prueba", 1, PeriodoLectivo.SEGUNDO_CUATRIMESTRE);
        baseDeDatos = insertarMateria(plan, "Base de Datos de prueba", 1, PeriodoLectivo.SEGUNDO_CUATRIMESTRE);
        ingles = insertarMateria(plan, "Inglés I de prueba", 1, PeriodoLectivo.PRIMER_CUATRIMESTRE);
        analisis = insertarMateria(plan, "Análisis de prueba", 2, PeriodoLectivo.ANUAL);
        deOtroPlan = insertarMateria(insertarPlan(carrera, "Plan 2003"), "Materia del plan viejo", 1,
                PeriodoLectivo.SEGUNDO_CUATRIMESTRE);

        titular = insertarPersona("Etcheber", "Esteban", "30000021", 990021L, true);
        ayudante = insertarPersona("Nieto", "Nora", "30000022", 990022L, true);
        otroDocente = insertarPersona("Monaco", "Emilio", "30000023", 990023L, true);
        docenteDadoDeBaja = insertarPersona("Paz", "Luis", "30000024", 990024L, false);
        estudiante = insertarPersona("Díaz", "Sol", "40000025", null, true);

        laboratorioA = aulaRepository.save(new Aula("Laboratorio A prueba", 30));
        laboratorioB = aulaRepository.save(new Aula("Laboratorio B prueba", 30));
        Aula cerrada = new Aula("Aula cerrada prueba", 20);
        cerrada.darDeBaja(LocalDate.of(2025, 3, 1));
        aulaCerrada = aulaRepository.save(cerrada);

        comision = comisionRepository.save(new Comision(plan, 1, 1, Turno.MANANA, 2026));
        otraComision = new Comision(plan, 1, 2, Turno.MANANA, 2026);
        ComisionMateria bd = otraComision.agregarMateria(baseDeDatos, PeriodoLectivo.SEGUNDO_CUATRIMESTRE, null);
        bd.asignarDocente(otroDocente, RolDocente.TITULAR);
        bd.agregarHorario(DiaSemana.LUNES, LocalTime.of(8, 30), LocalTime.of(10, 30), laboratorioA);
        comisionRepository.saveAndFlush(otraComision);
    }

    // --- Agregar ---

    @Test
    void agregaLaMateriaConSuPeriodoDocentesYHorarios() throws Exception {
        agregar(programacion, 40,
                List.of(docente(ayudante, "AYUDANTE"), docente(titular, "TITULAR")),
                List.of(horario("MARTES", "08:30", "10:30", laboratorioB.getId()),
                        horario("LUNES", "11:00", "13:00", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.materiaId").value(programacion))
                .andExpect(jsonPath("$.nombreMateria").value("Programación II de prueba"))
                .andExpect(jsonPath("$.periodoLectivo").value("SEGUNDO_CUATRIMESTRE"))
                .andExpect(jsonPath("$.cupoMaximo").value(40))
                .andExpect(jsonPath("$.docentes[*].apellido", contains("Etcheber", "Nieto")))
                .andExpect(jsonPath("$.horarios[*].diaSemana", contains("LUNES", "MARTES")))
                .andExpect(jsonPath("$.horarios[0].id", notNullValue()))
                .andExpect(jsonPath("$.horarios[1].aulaNombre").value("Laboratorio B prueba"));
    }

    @Test
    void sinDocentesNiHorariosTambienSePuedeAgregar() throws Exception {
        agregar(programacion, null, List.of(), List.of())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cupoMaximo").doesNotExist());
    }

    @Test
    void unaMateriaDeOtroPlanDevuelve409() throws Exception {
        agregar(deOtroPlan, null, List.of(), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La materia " + deOtroPlan + " no es del plan de estudio " + plan));
    }

    @Test
    void unaMateriaDeOtroAnioDeLaCarreraDevuelve409() throws Exception {
        agregar(analisis, null, List.of(), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Análisis de prueba es de 2° año y la comisión es de 1° año"));
    }

    @Test
    void unaMateriaQueYaEstaEnLaComisionDevuelve409() throws Exception {
        agregar(programacion, null, List.of(), List.of()).andExpect(status().isCreated());

        agregar(programacion, null, List.of(), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Programación II de prueba ya está en la comisión 1/26"));
    }

    @Test
    void unaComisionDadaDeBajaNoAceptaMaterias() throws Exception {
        mvc.perform(post("/api/admin/comisiones/{id}/baja", comision.getId()).with(admin()))
                .andExpect(status().isOk());

        agregar(programacion, null, List.of(), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La comisión " + comision.getId() + " está dada de baja"));
    }

    // --- Docentes ---

    @Test
    void dosTitularesDevuelve409() throws Exception {
        agregar(programacion, null, List.of(docente(titular, "TITULAR"), docente(ayudante, "TITULAR")), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La materia puede tener como máximo un titular"));
    }

    @Test
    void unDocenteRepetidoDevuelve409() throws Exception {
        agregar(programacion, null, List.of(docente(titular, "TITULAR"), docente(titular, "AYUDANTE")), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El docente " + titular + " está más de una vez"));
    }

    @Test
    void unaPersonaQueNoEsDocenteDevuelve404() throws Exception {
        agregar(programacion, null, List.of(docente(estudiante, "AYUDANTE")), List.of())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el docente con id " + estudiante));
    }

    @Test
    void unDocenteDadoDeBajaDevuelve409() throws Exception {
        agregar(programacion, null, List.of(docente(docenteDadoDeBaja, "TITULAR")), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El docente Paz, Luis está dado de baja"));
    }

    @Test
    void unDocenteQueYaDaClaseEnOtraComisionALaMismaHoraDevuelve409() throws Exception {
        agregar(programacion, null, List.of(docente(otroDocente, "TITULAR")),
                List.of(horario("LUNES", "09:00", "11:00", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El docente Monaco, Emilio ya da clase el lunes de 09:00 a 11:00 en otra materia"));
    }

    // --- Horarios ---

    @Test
    void horaDeFinAnteriorALaDeInicioDevuelve400() throws Exception {
        agregar(programacion, null, List.of(), List.of(horario("LUNES", "10:30", "08:30", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("horarios[0].horaFinPosterior"));
    }

    @Test
    void horariosQueSePisanEntreSiDevuelve409() throws Exception {
        agregar(programacion, null, List.of(),
                List.of(horario("LUNES", "08:30", "10:30", null), horario("LUNES", "10:00", "12:00", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Los horarios del lunes de 08:30 a 10:30 y de 10:00 a 12:00 se superponen"));
    }

    @Test
    void chocarConOtraMateriaDeLaMismaComisionDevuelve409() throws Exception {
        agregar(programacion, null, List.of(), List.of(horario("MIERCOLES", "08:30", "10:30", null)))
                .andExpect(status().isCreated());

        agregar(baseDeDatos, null, List.of(), List.of(horario("MIERCOLES", "09:30", "11:30", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El horario del miercoles de 09:30 a 11:30 se superpone con "
                        + "Programación II de prueba, que la comisión cursa el miercoles de 08:30 a 10:30"));
    }

    @Test
    void materiasDeDistintoCuatrimestrePuedenCompartirHorario() throws Exception {
        agregar(programacion, null, List.of(), List.of(horario("MIERCOLES", "08:30", "10:30", laboratorioB.getId())))
                .andExpect(status().isCreated());

        agregar(ingles, null, List.of(), List.of(horario("MIERCOLES", "08:30", "10:30", laboratorioB.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void unAulaOcupadaPorOtraComisionDevuelve409() throws Exception {
        agregar(programacion, null, List.of(), List.of(horario("LUNES", "10:00", "12:00", laboratorioA.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El aula Laboratorio A prueba ya está ocupada el lunes de 10:00 a 12:00"));
    }

    @Test
    void franjasQueSoloSeTocanNoChocan() throws Exception {
        agregar(programacion, null, List.of(docente(otroDocente, "TITULAR")),
                List.of(horario("LUNES", "10:30", "12:30", laboratorioA.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void lasComisionesDadasDeBajaNoOcupanAulas() throws Exception {
        mvc.perform(post("/api/admin/comisiones/{id}/baja", otraComision.getId()).with(admin()))
                .andExpect(status().isOk());

        agregar(programacion, null, List.of(), List.of(horario("LUNES", "08:30", "10:30", laboratorioA.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void unAulaInexistenteDevuelve404() throws Exception {
        agregar(programacion, null, List.of(), List.of(horario("LUNES", "14:00", "16:00", -1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el aula con id -1"));
    }

    @Test
    void unAulaDadaDeBajaDevuelve409() throws Exception {
        agregar(programacion, null, List.of(), List.of(horario("LUNES", "14:00", "16:00", aulaCerrada.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El aula Aula cerrada prueba está dada de baja"));
    }

    // --- Modificar ---

    @Test
    void modificarReemplazaCupoDocentesYHorarios() throws Exception {
        long id = agregarId(programacion, 40, List.of(docente(titular, "TITULAR")),
                List.of(horario("MARTES", "08:30", "10:30", laboratorioB.getId()),
                        horario("JUEVES", "08:30", "10:30", laboratorioB.getId())));

        // El martes se reenvía igual: no choca consigo mismo ni con el UNIQUE de su versión vieja.
        modificar(id, 35, List.of(docente(titular, "AYUDANTE"), docente(ayudante, "TITULAR")),
                List.of(horario("MARTES", "08:30", "10:30", laboratorioB.getId()),
                        horario("VIERNES", "11:00", "13:00", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cupoMaximo").value(35))
                .andExpect(jsonPath("$.docentes[*].apellido", contains("Nieto", "Etcheber")))
                .andExpect(jsonPath("$.horarios[*].diaSemana", contains("MARTES", "VIERNES")))
                .andExpect(jsonPath("$.nombreMateria").value("Programación II de prueba"));

        mvc.perform(get("/api/admin/comisiones/{id}", comision.getId()).with(admin()))
                .andExpect(jsonPath("$.materias[0].horarios[*].diaSemana", contains("MARTES", "VIERNES")));
    }

    @Test
    void modificarUnaMateriaDeOtraComisionDevuelve404() throws Exception {
        long idDeOtra = otraComision.getMaterias().getFirst().getId();

        modificar(idDeOtra, null, List.of(), List.of())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "La comisión " + comision.getId() + " no tiene la materia " + idDeOtra));
    }

    // --- Quitar ---

    @Test
    void quitarBorraLaMateriaDeLaComision() throws Exception {
        long id = agregarId(programacion, null, List.of(docente(titular, "TITULAR")),
                List.of(horario("MARTES", "08:30", "10:30", null)));

        mvc.perform(delete(URL + "/{id}", comision.getId(), id).with(admin()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/comisiones/{id}", comision.getId()).with(admin()))
                .andExpect(jsonPath("$.materias.length()").value(0));
    }

    @Test
    void quitarUnaMateriaConHistoriaDevuelve409() throws Exception {
        long id = agregarId(programacion, null, List.of(), List.of());
        jdbc.update("""
                INSERT INTO encuesta (comision_materia_id, fecha_apertura, fecha_cierre)
                VALUES (?, '2026-11-01 00:00:00', '2026-11-30 00:00:00')
                """, id);

        mvc.perform(delete(URL + "/{id}", comision.getId(), id).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La materia " + id + " de la comisión " + comision.getId()
                        + " tiene inscripciones, evaluaciones o encuestas: no se puede quitar"));
    }

    // --- Ayudantes ---

    private ResultActions agregar(Long materiaId, Integer cupo, List<String> docentes, List<String> horarios)
            throws Exception {
        return mvc.perform(post(URL, comision.getId()).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"materiaId": %d, "cupoMaximo": %s, "docentes": [%s], "horarios": [%s]}
                        """.formatted(materiaId, cupo, String.join(",", docentes), String.join(",", horarios))));
    }

    private long agregarId(Long materiaId, Integer cupo, List<String> docentes, List<String> horarios)
            throws Exception {
        String respuesta = agregar(materiaId, cupo, docentes, horarios)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(respuesta, "$.id").longValue();
    }

    private ResultActions modificar(long comisionMateriaId, Integer cupo, List<String> docentes,
                                    List<String> horarios) throws Exception {
        return mvc.perform(put(URL + "/{id}", comision.getId(), comisionMateriaId).with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"cupoMaximo": %s, "docentes": [%s], "horarios": [%s]}
                        """.formatted(cupo, String.join(",", docentes), String.join(",", horarios))));
    }

    private static String docente(Long docenteId, String rol) {
        return """
                {"docenteId": %d, "rol": "%s"}""".formatted(docenteId, rol);
    }

    private static String horario(String dia, String inicio, String fin, Long aulaId) {
        return """
                {"diaSemana": "%s", "horaInicio": "%s", "horaFin": "%s", "aulaId": %s}""".formatted(dia, inicio, fin, aulaId);
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

    // Con legajoDocente null es una persona que no es docente.
    private Long insertarPersona(String apellido, String nombre, String dni, Long legajoDocente, boolean activo) {
        jdbc.update("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente,
                                     activo, fecha_baja)
                VALUES (?, ?, ?, '2231234567', '1980-01-01', ?, ?, ?, ?)
                """, nombre, apellido, dni, dni + "@atenea.test", legajoDocente, activo,
                activo ? null : LocalDate.of(2025, 3, 1));
        return ultimoId();
    }

    private Long ultimoId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
