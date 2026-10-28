package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.inscripciones.dto.MateriaPlanEstudianteResponse;
import com.utn.atenea.inscripciones.dto.PlanEstudianteResponse;
import com.utn.atenea.inscripciones.reglas.EstadoMateria;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** El plan de estudio del estudiante con el estado de cada materia y su avance en la carrera. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanEstudianteService {

    private final PersonasApi personasApi;
    private final CarrerasApi carrerasApi;
    private final SituacionAcademicaService situacionAcademicaService;

    /** Uno por cada inscripción a carrera activa; las materias, por año, período y nombre. */
    public List<PlanEstudianteResponse> planes(Long estudianteId) {
        return personasApi.inscripcionesCarrera(estudianteId).stream()
                .filter(InscripcionCarreraResumen::activa)
                .map(this::plan)
                .toList();
    }

    private PlanEstudianteResponse plan(InscripcionCarreraResumen inscripcionCarrera) {
        PlanEstudioResumen plan = carrerasApi.buscarPlan(inscripcionCarrera.planEstudioId()).orElseThrow();
        SituacionAcademica situacion = situacionAcademicaService.de(inscripcionCarrera.id(), plan.id());
        List<MateriaPlanEstudianteResponse> materias = carrerasApi.materiasDelPlan(plan.id()).stream()
                .map(materia -> new MateriaPlanEstudianteResponse(materia.materiaId(), materia.nombre(),
                        materia.anioCursado(), materia.periodoLectivo(), situacion.estado(materia.materiaId()),
                        situacion.notaDeAprobacion(materia.materiaId()).orElse(null),
                        situacion.vencimientoDeLaRegularidad(materia.materiaId()).orElse(null)))
                .toList();
        int aprobadas = (int) materias.stream()
                .filter(materia -> materia.estado() == EstadoMateria.APROBADA)
                .count();
        return new PlanEstudianteResponse(plan.id(), plan.nombreCarrera(), plan.nombre(), aprobadas, materias.size(),
                porcentaje(aprobadas, materias.size()), materias);
    }

    // Para abajo: no muestra 100 % hasta que aprueba la última.
    private static int porcentaje(int aprobadas, int total) {
        return total == 0 ? 0 : aprobadas * 100 / total;
    }
}
