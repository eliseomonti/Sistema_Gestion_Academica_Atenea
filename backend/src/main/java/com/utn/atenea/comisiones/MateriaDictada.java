package com.utn.atenea.comisiones;

import com.utn.atenea.carreras.PeriodoLectivo;

import java.time.LocalTime;
import java.util.List;

/**
 * Una materia que se dicta en una comisión activa, con lo que necesita ver quien elige dónde cursarla.
 * nombreComision es el de la planilla ("1/25"); cupoMaximo en null es sin límite. Los horarios vienen
 * por día y hora; los docentes, el titular primero.
 */
public record MateriaDictada(
        Long comisionMateriaId,
        String nombreComision,
        Turno turno,
        Long materiaId,
        PeriodoLectivo periodoLectivo,
        Integer cupoMaximo,
        List<Horario> horarios,
        List<Docente> docentes
) {

    /** aula en null si la cursada de esa franja es virtual. */
    public record Horario(DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin, String aula) {
    }

    /** docenteId es el personaId: el nombre se le pide a personas. */
    public record Docente(Long docenteId, RolDocente rol) {
    }
}
