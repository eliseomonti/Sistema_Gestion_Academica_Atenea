package com.utn.atenea.personas.service;

import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import com.utn.atenea.personas.model.InscripcionCarrera;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.InscripcionCarreraRepository;
import com.utn.atenea.personas.repository.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Contra la base real; {@code @Transactional} deshace las personas de prueba. */
@SpringBootTest
@Transactional
class PersonasApiImplTest {

    @Autowired PersonasApi personasApi;
    @Autowired PersonaRepository personaRepository;
    @Autowired InscripcionCarreraRepository inscripcionCarreraRepository;
    @Autowired JdbcTemplate jdbc;

    Persona docente;
    Persona docenteDadoDeBaja;
    Persona estudiante;

    @BeforeEach
    void crearPersonas() {
        docente = new Persona("Ana", "Ruiz", "30000001", "2231111111", LocalDate.of(1980, 1, 1), "ana.api@atenea.test");
        docente.asignarLegajoDocente(990001L);
        personaRepository.save(docente);

        docenteDadoDeBaja = new Persona("Luis", "Paz", "30000002", "2232222222", LocalDate.of(1975, 1, 1), "luis.api@atenea.test");
        docenteDadoDeBaja.asignarLegajoDocente(990002L);
        docenteDadoDeBaja.darDeBaja(LocalDate.of(2025, 3, 1));
        personaRepository.save(docenteDadoDeBaja);

        estudiante = new Persona("Sol", "Díaz", "40000003", "2233333333", LocalDate.of(2005, 1, 1), "sol.api@atenea.test");
        estudiante.asignarLegajoEstudiante(990003L);
        personaRepository.save(estudiante);
    }

    @Test
    void devuelveLosDocentesActivosYDadosDeBaja() {
        assertThat(personasApi.buscarDocentes(List.of(docente.getId(), docenteDadoDeBaja.getId())))
                .containsExactlyInAnyOrder(
                        new DocenteResumen(docente.getId(), "Ana", "Ruiz", 990001L, true),
                        new DocenteResumen(docenteDadoDeBaja.getId(), "Luis", "Paz", 990002L, false));
    }

    @Test
    void noDevuelveALasPersonasQueNoSonDocentesNiALasQueNoExisten() {
        assertThat(personasApi.buscarDocentes(List.of(docente.getId(), estudiante.getId(), -1L)))
                .extracting(DocenteResumen::personaId)
                .containsExactly(docente.getId());
    }

    @Test
    void sinIdsDevuelveListaVacia() {
        assertThat(personasApi.buscarDocentes(List.of())).isEmpty();
    }

    @Test
    void inscripcionesCarreraDevuelveLasActivasYLasQueNo() {
        Long planActivo = insertarPlan("Carrera de prueba api personas 1");
        Long planDadoDeBaja = insertarPlan("Carrera de prueba api personas 2");
        InscripcionCarrera activa = inscripcionCarreraRepository.save(
                new InscripcionCarrera(estudiante, planActivo, LocalDate.of(2024, 3, 1)));
        InscripcionCarrera dadaDeBaja = new InscripcionCarrera(estudiante, planDadoDeBaja, LocalDate.of(2023, 3, 1));
        dadaDeBaja.darDeBaja(LocalDate.of(2023, 9, 1));
        inscripcionCarreraRepository.save(dadaDeBaja);

        assertThat(personasApi.inscripcionesCarrera(estudiante.getId()))
                .containsExactlyInAnyOrder(
                        new InscripcionCarreraResumen(activa.getId(), planActivo, true),
                        new InscripcionCarreraResumen(dadaDeBaja.getId(), planDadoDeBaja, false));
        assertThat(personasApi.inscripcionesCarrera(docente.getId())).isEmpty();
    }

    // carrera y plan_estudio son del módulo carreras: se insertan con SQL.
    private Long insertarPlan(String nombreCarrera) {
        jdbc.update("""
                INSERT INTO carrera (nombre, duracion, titulo_otorgado, modalidad_carrera)
                VALUES (?, '2 años', 'Técnico', 'PRESENCIAL')
                """, nombreCarrera);
        jdbc.update("INSERT INTO plan_estudio (carrera_id, nombre) VALUES (LAST_INSERT_ID(), 'Plan 2024')");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
