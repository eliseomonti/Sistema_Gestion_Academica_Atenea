package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.comisiones.AulaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.examenes.ExamenFinalResumen;
import com.utn.atenea.examenes.ExamenesApi;
import com.utn.atenea.inscripciones.dto.DocenteMesaResponse;
import com.utn.atenea.inscripciones.dto.OfertaFinalResponse;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionFinal;
import com.utn.atenea.inscripciones.model.PeriodoInscripcion;
import com.utn.atenea.inscripciones.model.TipoPeriodo;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica;
import com.utn.atenea.inscripciones.repository.InscripcionFinalRepository;
import com.utn.atenea.inscripciones.repository.PeriodoInscripcionRepository;
import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las mesas a las que el estudiante se puede anotar: las de los turnos abiertos de sus carreras, de
 * materias de su plan que puede rendir según su situación académica. Es un solo caso de uso: las 7
 * dependencias no justifican partirlo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfertaFinalService {

    private final PeriodoInscripcionRepository periodoInscripcionRepository;
    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final ExamenesApi examenesApi;
    private final ComisionesApi comisionesApi;
    private final PersonasApi personasApi;
    private final CarrerasApi carrerasApi;
    private final SituacionAcademicaService situacionAcademicaService;

    /** Por plan; dentro de cada plan, por fecha y hora de la mesa. */
    public List<OfertaFinalResponse> ofertar(Long estudianteId) {
        LocalDateTime ahora = LocalDateTime.now();
        return personasApi.inscripcionesCarrera(estudianteId).stream()
                .filter(InscripcionCarreraResumen::activa)
                .flatMap(inscripcionCarrera -> ofertaDelPlan(inscripcionCarrera, ahora).stream())
                .toList();
    }

    private List<OfertaFinalResponse> ofertaDelPlan(InscripcionCarreraResumen inscripcionCarrera, LocalDateTime ahora) {
        Long planEstudioId = inscripcionCarrera.planEstudioId();
        Long carreraId = carrerasApi.buscarPlan(planEstudioId).map(PlanEstudioResumen::carreraId).orElseThrow();
        List<Long> turnosAbiertos = periodoInscripcionRepository.abiertos(carreraId, TipoPeriodo.FINAL, ahora).stream()
                .map(PeriodoInscripcion::getId)
                .toList();
        if (turnosAbiertos.isEmpty()) {
            return List.of();
        }
        Map<Long, String> materiasDelPlan = carrerasApi.materiasDelPlan(planEstudioId).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
        List<ExamenFinalResumen> delPlan = examenesApi.examenesFinalesDeLosTurnos(turnosAbiertos).stream()
                .filter(mesa -> materiasDelPlan.containsKey(mesa.materiaId()))
                .toList();
        if (delPlan.isEmpty()) {
            return List.of();
        }

        // Las que puede rendir, y las que ya tiene: ésas siguen para que vea dónde está anotado y pueda
        // darse de baja, aunque su situación haya cambiado después.
        Set<Long> inscriptoEn = inscripcionFinalRepository
                .findByInscripcionCarreraIdAndEstado(inscripcionCarrera.id(), EstadoInscripcion.INSCRIPTO).stream()
                .map(InscripcionFinal::getExamenFinalId)
                .collect(Collectors.toSet());
        SituacionAcademica situacion = situacionAcademicaService.de(inscripcionCarrera.id(), planEstudioId);
        List<ExamenFinalResumen> mesas = delPlan.stream()
                .filter(mesa -> inscriptoEn.contains(mesa.id()) || situacion.puedeRendir(mesa.materiaId(), mesa.fecha()))
                .toList();
        if (mesas.isEmpty()) {
            return List.of();
        }

        // Lo que la oferta muestra y no sabe examenes se pide una sola vez, para todo el plan.
        Map<Long, String> aulas = comisionesApi
                .buscarAulas(mesas.stream().map(ExamenFinalResumen::aulaId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(AulaResumen::id, AulaResumen::nombre));
        Map<Long, DocenteResumen> docentes = personasApi
                .buscarDocentes(mesas.stream().flatMap(mesa -> mesa.docenteIds().stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(DocenteResumen::personaId, Function.identity()));

        return mesas.stream()
                .map(mesa -> new OfertaFinalResponse(planEstudioId, mesa.id(), mesa.materiaId(),
                        materiasDelPlan.get(mesa.materiaId()), mesa.fecha(), mesa.horaInicio(), mesa.horaFin(),
                        mesa.aulaId() != null ? aulas.get(mesa.aulaId()) : null,
                        tribunal(mesa, docentes), inscriptoEn.contains(mesa.id())))
                .toList();
    }

    private static List<DocenteMesaResponse> tribunal(ExamenFinalResumen mesa, Map<Long, DocenteResumen> docentes) {
        return mesa.docenteIds().stream()
                .map(docentes::get)
                .sorted(Comparator.comparing(DocenteResumen::apellido).thenComparing(DocenteResumen::nombre))
                .map(docente -> new DocenteMesaResponse(docente.apellido(), docente.nombre()))
                .toList();
    }
}
