package com.utn.atenea.examenes.service;

import com.utn.atenea.examenes.CondicionCursada;
import com.utn.atenea.examenes.ExamenFinalResumen;
import com.utn.atenea.examenes.ExamenesApi;
import com.utn.atenea.examenes.ResultadoCursada;
import com.utn.atenea.examenes.ResultadoFinal;
import com.utn.atenea.examenes.model.CierreCursada;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.examenes.model.NotaFinal;
import com.utn.atenea.examenes.repository.CierreCursadaRepository;
import com.utn.atenea.examenes.repository.ExamenFinalRepository;
import com.utn.atenea.examenes.repository.NotaFinalRepository;
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
class ExamenesApiImplTest {

    static final LocalDate HOY = LocalDate.of(2026, 7, 10);

    @Autowired ExamenesApi examenesApi;
    @Autowired CierreCursadaRepository cierreRepository;
    @Autowired ExamenFinalRepository examenFinalRepository;
    @Autowired NotaFinalRepository notaFinalRepository;
    @Autowired JdbcTemplate jdbc;

    Long carreraId;
    Long plan;
    Long materia;
    Long docente;

    @BeforeEach
    void crearDatos() {
        carreraId = insertar("INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera) "
                + "VALUES ('Carrera de prueba api examenes', '2 años', 'Técnico', 'PRESENCIAL')");
        plan = insertar("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (?, 'Plan 2024')", carreraId);
        materia = insertar("INSERT INTO materia (nombre, carga_horaria_semanal, carga_horaria_total) "
                + "VALUES ('Álgebra de prueba api examenes', 4, 64)");
        docente = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_docente)
                VALUES ('Ana', 'Ruiz', '30000911', '2231111111', '1980-01-01', 'ana@api-examenes.test', 980911)
                """);
    }

    @Test
    void cierresVigentesDevuelveLaUltimaActaDeCadaCursada() {
        Long comisionMateria = insertar("INSERT INTO comision_materia (comision_id, materia_id, periodo_lectivo) "
                + "VALUES (?, ?, 'PRIMER_CUATRIMESTRE')", insertar("INSERT INTO comision (plan_estudio_id, "
                + "anio_cursado, nro_comision, turno, anio_lectivo) VALUES (?, 1, 1, 'MANANA', 2026)", plan), materia);
        Long periodo = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'CURSADA', 2026, 'PRIMER_CUATRIMESTRE', '2026-02-01', '2026-03-01')
                """, carreraId);
        Long cursada = insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, "
                + "comision_materia_id, periodo_id) VALUES (?, ?, ?)", nuevaInscripcionCarrera("40000911"),
                comisionMateria, periodo);
        Long sinActa = insertar("INSERT INTO estudiante_inscripcion_comision (inscripcion_carrera_id, "
                + "comision_materia_id, periodo_id) VALUES (?, ?, ?)", nuevaInscripcionCarrera("40000912"),
                comisionMateria, periodo);
        CierreCursada original = cierreRepository.save(new CierreCursada(cursada, docente,
                CondicionCursada.NO_REGULAR, 3, HOY, null, HOY.atStartOfDay()));
        cierreRepository.save(original.rectificar(docente, CondicionCursada.REGULAR, 5, HOY, HOY.plusYears(2),
                LocalDateTime.now()));

        assertThat(examenesApi.cierresVigentes(List.of(cursada, sinActa)))
                .containsExactly(new ResultadoCursada(cursada, CondicionCursada.REGULAR, 5, HOY, HOY.plusYears(2)));
        assertThat(examenesApi.cierresVigentes(List.of())).isEmpty();
    }

    @Test
    void elFinalSeApruebaConSeis() {
        ExamenFinal mesa = examenFinalRepository.save(new ExamenFinal(materia, insertarPeriodoFinal(), HOY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), null));
        Long conSeis = inscribirAlFinal("40000913", mesa);
        Long conCinco = inscribirAlFinal("40000914", mesa);
        Long ausente = inscribirAlFinal("40000915", mesa);
        notaFinalRepository.save(new NotaFinal(conSeis, docente, 6, HOY));
        notaFinalRepository.save(new NotaFinal(conCinco, docente, 5, HOY));
        notaFinalRepository.save(new NotaFinal(ausente, docente, null, HOY));

        assertThat(examenesApi.notasFinalesVigentes(List.of(conSeis, conCinco, ausente)))
                .containsExactlyInAnyOrder(
                        new ResultadoFinal(conSeis, 6, true),
                        new ResultadoFinal(conCinco, 5, false),
                        new ResultadoFinal(ausente, null, false));
    }

    @Test
    void buscarExamenesFinalesDevuelveLaMesaConSuTribunal() {
        Long periodo = insertarPeriodoFinal();
        ExamenFinal mesa = new ExamenFinal(materia, periodo, HOY, LocalTime.of(9, 0), LocalTime.of(12, 0), null);
        mesa.cambiarDocentes(List.of(docente));
        examenFinalRepository.save(mesa);

        assertThat(examenesApi.buscarExamenesFinales(List.of(mesa.getId(), -1L)))
                .containsExactly(new ExamenFinalResumen(mesa.getId(), materia, periodo, HOY, LocalTime.of(9, 0),
                        LocalTime.of(12, 0), null, Set.of(docente)));
        assertThat(examenesApi.buscarExamenesFinales(List.of())).isEmpty();
    }

    @Test
    void examenesFinalesDeLosTurnosVienenPorFechaYHora() {
        Long periodo = insertarPeriodoFinal();
        Long otroTurno = insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', 2026, '2026-11-01', '2026-11-05')
                """, carreraId);
        ExamenFinal tarde = examenFinalRepository.save(new ExamenFinal(materia, periodo, HOY,
                LocalTime.of(14, 0), LocalTime.of(16, 0), null));
        ExamenFinal temprano = examenFinalRepository.save(new ExamenFinal(materia, periodo, HOY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), null));
        examenFinalRepository.save(new ExamenFinal(materia, otroTurno, HOY.plusMonths(4),
                LocalTime.of(9, 0), LocalTime.of(12, 0), null));

        assertThat(examenesApi.examenesFinalesDeLosTurnos(List.of(periodo)))
                .extracting(ExamenFinalResumen::id)
                .containsExactly(temprano.getId(), tarde.getId());
        assertThat(examenesApi.examenesFinalesDeLosTurnos(List.of())).isEmpty();
    }

    private Long insertarPeriodoFinal() {
        return insertar("""
                INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, fecha_inicio, fecha_cierre)
                VALUES (?, 'FINAL', 2026, '2026-07-01', '2026-07-05')
                """, carreraId);
    }

    private Long inscribirAlFinal(String dni, ExamenFinal mesa) {
        return insertar("INSERT INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id) "
                + "VALUES (?, ?)", nuevaInscripcionCarrera(dni), mesa.getId());
    }

    private Long nuevaInscripcionCarrera(String dni) {
        Long estudiante = insertar("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email, legajo_estudiante)
                VALUES ('Sol', 'Díaz', ?, '2233333333', '2005-01-01', ?, ?)
                """, dni, dni + "@api-examenes.test", Long.valueOf("99" + dni.substring(4)));
        return insertar("INSERT INTO inscripcion_carrera (estudiante_id, plan_estudio_id) VALUES (?, ?)",
                estudiante, plan);
    }

    private Long insertar(String sql, Object... parametros) {
        jdbc.update(sql, parametros);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
