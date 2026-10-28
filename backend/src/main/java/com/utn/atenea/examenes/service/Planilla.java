package com.utn.atenea.examenes.service;

import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.dto.NotaEstudianteResponse;
import com.utn.atenea.examenes.model.CondicionExamen;
import com.utn.atenea.personas.EstudianteResumen;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lo que comparten las planillas del docente: la de una evaluación, la de una mesa y el acta de cierre. */
final class Planilla {

    static final Comparator<NotaEstudianteResponse> POR_APELLIDO =
            Comparator.comparing(NotaEstudianteResponse::apellido).thenComparing(NotaEstudianteResponse::nombre);

    private Planilla() {
    }

    /** Cada inscripción una sola vez, y todas de estudiantes anotados que no se dieron de baja. */
    static void validarInscripciones(List<Long> inscripcionIds, Set<Long> inscripcionesAnotadas) {
        Set<Long> vistas = new HashSet<>();
        for (Long inscripcionId : inscripcionIds) {
            if (!vistas.add(inscripcionId)) {
                throw new ReglaNegocioException("La inscripción " + inscripcionId + " está más de una vez");
            }
            if (!inscripcionesAnotadas.contains(inscripcionId)) {
                throw new ReglaNegocioException("La inscripción " + inscripcionId
                        + " no es de un estudiante anotado que no se haya dado de baja");
            }
        }
    }

    /** condicion y nota en null: todavía no está cargada. */
    static NotaEstudianteResponse fila(Long inscripcionId, EstudianteResumen estudiante, CondicionExamen condicion,
                                       Integer nota) {
        return new NotaEstudianteResponse(inscripcionId, estudiante.personaId(), estudiante.legajoEstudiante(),
                estudiante.apellido(), estudiante.nombre(), condicion, nota);
    }
}
