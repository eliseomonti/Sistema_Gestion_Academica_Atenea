package com.utn.atenea.examenes.repository;

import com.utn.atenea.examenes.CondicionCursada;
import com.utn.atenea.examenes.model.CierreCursada;
import com.utn.atenea.examenes.model.CondicionExamen;
import com.utn.atenea.examenes.model.EvaluacionCursada;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.examenes.model.NotaEvaluacion;
import com.utn.atenea.examenes.model.NotaFinal;
import com.utn.atenea.examenes.model.TipoEvaluacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra la base real; {@code @Transactional} deshace los datos de prueba. Lo de otros módulos se
 * inserta con SQL.
 */
@SpringBootTest
@Transactional
class ExamenesRepositoryTest {

    static final LocalDate HOY = LocalDate.of(2026, 7, 10);
    static final LocalDateTime AHORA = HOY.atTime(12, 0);

    @Autowired EvaluacionCursadaRepository evaluacionRepository;
    @Autowired NotaEvaluacionRepository notaEvaluacionRepository;
    @Autowired CierreCursadaRepository cierreRepository;
    @Autowired ExamenFinalRepository examenFinalRepository;
    @Autowired NotaFinalRepository notaFinalRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    Long carreraId;
    Long materia;
    Long comisionMateria;
    Long docente;
    Long inscripcionCarrera1;
    Long inscripcionCarrera2;
    Long inscripcionComision1;
    Long inscripcionComision2;

    @BeforeEach
    void crearDatos() {
        carreraId = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba examenes', '2 años', 'Técnico', 'PRESENCIAL')");
        Long plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carreraId);
        materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Programación I de prueba examenes', 4, 64)");
        Long comision = insertar("INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo) "
                + "VALUES (?, 1, 1, 'MANANA', 2026)", plan);
        comisionMateria = insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo) "
                + "VALUES (?, ?, 'PRIMER_CUATRIMESTRE')", comision, materia);
        docente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Ana', 'Ruiz', '30000901', '2231111111', '1980-01-01', 'ana@examenes.test', 980901)
                """);
        inscripcionCarrera1 = insertarInscripcionCarrera(insertarEstudiante("40000901", 990901L), plan);
        inscripcionCarrera2 = insertarInscripcionCarrera(insertarEstudiante("40000902", 990902L), plan);

        Long periodoCursada = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', 2026, 'PRIMER_CUATRIMESTRE', '2026-02-01', '2026-03-01')
                """, carreraId);
        inscripcionComision1 = insertarInscripcionComision(inscripcionCarrera1, periodoCursada);
        inscripcionComision2 = insertarInscripcionComision(inscripcionCarrera2, periodoCursada);
    }

    @Test
    void elCierreVigenteEsElUltimoDeLaCadena() {
        CierreCursada original = cierreRepository.save(new CierreCursada(inscripcionComision1, docente,
                CondicionCursada.REGULAR, 6, HOY, HOY.plusYears(2), AHORA));
        CierreCursada rectificado = cierreRepository.save(original.rectificar(docente,
                CondicionCursada.PROMOCIONADO, 8, HOY, null, AHORA.plusDays(1)));
        CierreCursada delOtro = cierreRepository.save(new CierreCursada(inscripcionComision2, docente,
                CondicionCursada.NO_REGULAR, null, HOY, null, AHORA));

        assertThat(cierreRepository.vigente(inscripcionComision1)).contains(rectificado);
        assertThat(rectificado.getInscripcionComisionId()).isEqualTo(inscripcionComision1);
        assertThat(cierreRepository.vigentes(List.of(inscripcionComision1, inscripcionComision2, -1L)))
                .containsExactlyInAnyOrder(rectificado, delOtro);
    }

    @Test
    void sinActaNoHayCierreVigente() {
        assertThat(cierreRepository.vigente(inscripcionComision1)).isEmpty();
    }

    @Test
    void laNotaDeEvaluacionVigenteEsLaUltimaDeLaCadena() {
        EvaluacionCursada parcial = evaluacionRepository.save(
                new EvaluacionCursada(comisionMateria, "Primer parcial", HOY, TipoEvaluacion.PARCIAL));
        EvaluacionCursada recuperatorio = evaluacionRepository.save(
                parcial.recuperatorio("Recuperatorio primer parcial", HOY.plusDays(14)));

        NotaEvaluacion ausente = notaEvaluacionRepository.save(
                new NotaEvaluacion(parcial, inscripcionComision1, docente, null, HOY));
        NotaEvaluacion corregida = notaEvaluacionRepository.save(ausente.rectificar(docente, 2, HOY));
        NotaEvaluacion enElRecuperatorio = notaEvaluacionRepository.save(
                new NotaEvaluacion(recuperatorio, inscripcionComision1, docente, 7, HOY.plusDays(14)));
        NotaEvaluacion delOtro = notaEvaluacionRepository.save(
                new NotaEvaluacion(parcial, inscripcionComision2, docente, 9, HOY));

        assertThat(ausente.getCondicion()).isEqualTo(CondicionExamen.AUSENTE);
        assertThat(corregida.getCondicion()).isEqualTo(CondicionExamen.PRESENTE);
        assertThat(recuperatorio.esRecuperatorio()).isTrue();
        assertThat(recuperatorio.getTipo()).isEqualTo(TipoEvaluacion.PARCIAL);
        assertThat(notaEvaluacionRepository.vigente(parcial.getId(), inscripcionComision1)).contains(corregida);
        assertThat(notaEvaluacionRepository.vigentesDeLaEvaluacion(parcial.getId()))
                .containsExactlyInAnyOrder(corregida, delOtro);
        assertThat(notaEvaluacionRepository.vigentes(List.of(inscripcionComision1)))
                .containsExactly(corregida, enElRecuperatorio);
    }

    @Test
    void nombreEnUsoEsPorMateriaDeComisionYExcluyeALaQueSeModifica() {
        EvaluacionCursada parcial = evaluacionRepository.save(
                new EvaluacionCursada(comisionMateria, "Primer parcial", HOY, TipoEvaluacion.PARCIAL));

        assertThat(evaluacionRepository.nombreEnUso(comisionMateria, "Primer parcial", null)).isTrue();
        assertThat(evaluacionRepository.nombreEnUso(comisionMateria, "Primer parcial", parcial.getId())).isFalse();
        assertThat(evaluacionRepository.nombreEnUso(comisionMateria, "Segundo parcial", null)).isFalse();
    }

    @Test
    void laNotaFinalVigenteEsLaUltimaDeLaCadena() {
        ExamenFinal mesa = examenFinalRepository.save(nuevaMesa(insertarPeriodoFinal()));
        Long inscripcionFinal1 = insertarInscripcionFinal(inscripcionCarrera1, mesa.getId());
        Long inscripcionFinal2 = insertarInscripcionFinal(inscripcionCarrera2, mesa.getId());

        NotaFinal original = notaFinalRepository.save(new NotaFinal(inscripcionFinal1, docente, 4, HOY));
        NotaFinal primeraCorreccion = notaFinalRepository.save(original.rectificar(docente, 5, HOY));
        NotaFinal segundaCorreccion = notaFinalRepository.save(primeraCorreccion.rectificar(docente, null, HOY));
        NotaFinal delOtro = notaFinalRepository.save(new NotaFinal(inscripcionFinal2, docente, 9, HOY));

        assertThat(notaFinalRepository.vigente(inscripcionFinal1)).contains(segundaCorreccion);
        assertThat(segundaCorreccion.getCondicion()).isEqualTo(CondicionExamen.AUSENTE);
        assertThat(notaFinalRepository.vigentes(List.of(inscripcionFinal1, inscripcionFinal2)))
                .containsExactlyInAnyOrder(segundaCorreccion, delOtro);
    }

    @Test
    void lasMesasDelTurnoVienenConSuTribunal() {
        Long otroDocente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Luis', 'Paz', '30000902', '2232222222', '1975-01-01', 'luis@examenes.test', 980902)
                """);
        Long periodo = insertarPeriodoFinal();
        ExamenFinal mesa = nuevaMesa(periodo);
        mesa.cambiarDocentes(List.of(docente, otroDocente));
        examenFinalRepository.save(mesa);
        // Que la lista venga de la base y no del persistence context.
        entityManager.flush();
        entityManager.clear();

        assertThat(examenFinalRepository.findByPeriodoIdOrderByFechaAscHoraInicioAsc(periodo))
                .singleElement()
                .satisfies(leida -> assertThat(leida.getDocenteIds()).isEqualTo(Set.of(docente, otroDocente)));
    }

    @Test
    void horarioEnUsoEsPorMateriaFechaYHoraDeInicioYExcluyeALaQueSeModifica() {
        ExamenFinal mesa = examenFinalRepository.save(nuevaMesa(insertarPeriodoFinal()));

        assertThat(examenFinalRepository.horarioEnUso(materia, HOY, LocalTime.of(9, 0), null)).isTrue();
        assertThat(examenFinalRepository.horarioEnUso(materia, HOY, LocalTime.of(9, 0), mesa.getId())).isFalse();
        assertThat(examenFinalRepository.horarioEnUso(materia, HOY, LocalTime.of(14, 0), null)).isFalse();
    }

    private ExamenFinal nuevaMesa(Long periodoId) {
        return new ExamenFinal(materia, periodoId, HOY, LocalTime.of(9, 0), LocalTime.of(12, 0), null);
    }

    private Long insertarPeriodoFinal() {
        return insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', 2026, '2026-07-01', '2026-07-05')
                """, carreraId);
    }

    private Long insertarEstudiante(String dni, Long legajo) {
        return insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', 'Díaz', ?, '2233333333', '2005-01-01', ?, ?)
                """, dni, dni + "@examenes.test", legajo);
    }

    private Long insertarInscripcionCarrera(Long estudianteId, Long planId) {
        return insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)",
                estudianteId, planId);
    }

    private Long insertarInscripcionComision(Long inscripcionCarreraId, Long periodoId) {
        return insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, comision_materia_id, "
                + "periodo_id) VALUES (?, ?, ?)", inscripcionCarreraId, comisionMateria, periodoId);
    }

    private Long insertarInscripcionFinal(Long inscripcionCarreraId, Long examenFinalId) {
        return insertar("INSERT INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id) "
                + "VALUES (?, ?)", inscripcionCarreraId, examenFinalId);
    }

    private Long insertar(String sql, Object... parametros) {
        jdbc.update(sql, parametros);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
