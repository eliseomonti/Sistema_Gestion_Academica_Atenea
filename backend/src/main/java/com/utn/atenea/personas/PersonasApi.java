package com.utn.atenea.personas;

import java.util.Collection;
import java.util.List;

public interface PersonasApi {

    /**
     * Los docentes con esos ids de persona, activos o dados de baja (activo lo distingue). Las
     * personas que no existen o no son docentes no vienen: quien llama compara contra los ids que pidió.
     */
    List<DocenteResumen> buscarDocentes(Collection<Long> personaIds);

    /** Las inscripciones a carrera del estudiante, en cualquier estado (activa lo distingue). */
    List<InscripcionCarreraResumen> inscripcionesCarrera(Long estudianteId);

    /** Las inscripciones a carrera con esos ids, en cualquier estado. Las que no existen no vienen. */
    List<InscripcionCarreraResumen> buscarInscripcionesCarrera(Collection<Long> inscripcionCarreraIds);

    /**
     * Los estudiantes con esos ids de persona, activos o dados de baja. Las personas que no existen o no
     * son estudiantes no vienen.
     */
    List<EstudianteResumen> buscarEstudiantes(Collection<Long> personaIds);
}
