package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.comisiones.MateriaDictada;
import com.utn.atenea.inscripciones.dto.OfertaComisionResponse;
import com.utn.atenea.inscripciones.dto.OfertaMateriaResponse;
import com.utn.atenea.inscripciones.mapper.OfertaComisionMapper;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import com.utn.atenea.inscripciones.model.PeriodoInscripcion;
import com.utn.atenea.inscripciones.model.TipoPeriodo;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica;
import com.utn.atenea.inscripciones.repository.CantidadInscriptos;
import com.utn.atenea.inscripciones.repository.InscripcionComisionRepository;
import com.utn.atenea.inscripciones.repository.PeriodoInscripcionRepository;
import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lo que el estudiante puede elegir para inscribirse: las materias de las comisiones de sus planes que
 * entran en un período de cursada abierto y que puede cursar según su situación académica, con
 * horarios, docentes y cupo libre. Es un solo caso de uso: las 7 dependencias no justifican partirlo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfertaComisionService {

    private final PeriodoInscripcionRepository periodoInscripcionRepository;
    private final InscripcionComisionRepository inscripcionComisionRepository;
    private final ComisionesApi comisionesApi;
    private final PersonasApi personasApi;
    private final CarrerasApi carrerasApi;
    private final SituacionAcademicaService situacionAcademicaService;
    private final OfertaComisionMapper ofertaComisionMapper;

    /** Por plan; dentro de cada plan, por año de la carrera, período y nombre de la materia. */
    public List<OfertaMateriaResponse> ofertar(Long estudianteId) {
        LocalDateTime ahora = LocalDateTime.now();
        return personasApi.inscripcionesCarrera(estudianteId).stream()
                .filter(InscripcionCarreraResumen::activa)
                .flatMap(inscripcionCarrera -> ofertaDelPlan(inscripcionCarrera, ahora).stream())
                .toList();
    }

    private List<OfertaMateriaResponse> ofertaDelPlan(InscripcionCarreraResumen inscripcionCarrera,
                                                      LocalDateTime ahora) {
        Long planEstudioId = inscripcionCarrera.planEstudioId();
        List<MateriaDictada> dictadas = dictadasEnPeriodosAbiertos(planEstudioId, ahora);
        if (dictadas.isEmpty()) {
            return List.of();
        }
        Set<Long> inscriptoEn = inscripcionComisionRepository
                .findByInscripcionCarreraIdInAndEstado(List.of(inscripcionCarrera.id()), EstadoInscripcion.INSCRIPTO)
                .stream()
                .map(InscripcionComision::getComisionMateriaId)
                .collect(Collectors.toSet());
        dictadas = queElEstudiantePuedeElegir(dictadas, inscripcionCarrera, inscriptoEn);
        if (dictadas.isEmpty()) {
            return List.of();
        }

        // Lo que la oferta muestra y no sabe comisiones se pide una sola vez, para todo el plan.
        List<Long> comisionMateriaIds = dictadas.stream().map(MateriaDictada::comisionMateriaId).toList();
        Map<Long, Long> inscriptos = inscripcionComisionRepository.contarInscriptos(comisionMateriaIds).stream()
                .collect(Collectors.toMap(CantidadInscriptos::comisionMateriaId, CantidadInscriptos::cantidad));
        Set<Long> docenteIds = dictadas.stream()
                .flatMap(materia -> materia.docentes().stream())
                .map(MateriaDictada.Docente::docenteId)
                .collect(Collectors.toSet());
        Map<Long, DocenteResumen> docentes = personasApi.buscarDocentes(docenteIds).stream()
                .collect(Collectors.toMap(DocenteResumen::personaId, Function.identity()));
        Map<Long, MateriaDelPlan> materiasDelPlan = carrerasApi.materiasDelPlan(planEstudioId).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, Function.identity()));

        // Agrupadas por materia, manteniendo el orden de las comisiones que da comisiones.
        Map<Long, List<OfertaComisionResponse>> comisionesPorMateria = new LinkedHashMap<>();
        for (MateriaDictada dictada : dictadas) {
            comisionesPorMateria.computeIfAbsent(dictada.materiaId(), materiaId -> new ArrayList<>())
                    .add(ofertaComisionMapper.toResponse(dictada, docentes, inscriptos, inscriptoEn));
        }
        return comisionesPorMateria.entrySet().stream()
                .map(entrada -> {
                    MateriaDelPlan materia = materiasDelPlan.get(entrada.getKey());
                    return new OfertaMateriaResponse(planEstudioId, materia.materiaId(), materia.nombre(),
                            materia.anioCursado(), materia.periodoLectivo(), entrada.getValue());
                })
                .sorted(Comparator.comparingInt(OfertaMateriaResponse::anioCursado)
                        .thenComparing(OfertaMateriaResponse::periodoLectivo)
                        .thenComparing(OfertaMateriaResponse::nombreMateria))
                .toList();
    }

    /**
     * Las que puede cursar según su situación académica, y las que ya está cursando: ésas siguen para que
     * vea en qué comisión está y pueda darse de baja, aunque una correlativa haya cambiado después.
     */
    private List<MateriaDictada> queElEstudiantePuedeElegir(List<MateriaDictada> dictadas,
                                                           InscripcionCarreraResumen inscripcionCarrera,
                                                           Set<Long> inscriptoEn) {
        SituacionAcademica situacion = situacionAcademicaService.de(inscripcionCarrera.id(),
                inscripcionCarrera.planEstudioId());
        Set<Long> materiasQueCursa = dictadas.stream()
                .filter(dictada -> inscriptoEn.contains(dictada.comisionMateriaId()))
                .map(MateriaDictada::materiaId)
                .collect(Collectors.toSet());
        return dictadas.stream()
                .filter(dictada -> materiasQueCursa.contains(dictada.materiaId())
                        || situacion.puedeCursar(dictada.materiaId()))
                .toList();
    }

    /** Las materias dictadas en el plan que admite algún período de cursada abierto de su carrera. */
    private List<MateriaDictada> dictadasEnPeriodosAbiertos(Long planEstudioId, LocalDateTime ahora) {
        Long carreraId = carrerasApi.buscarPlan(planEstudioId).map(PlanEstudioResumen::carreraId).orElseThrow();
        List<PeriodoInscripcion> abiertos = periodoInscripcionRepository.abiertos(carreraId, TipoPeriodo.CURSADA, ahora);
        return abiertos.stream()
                .map(PeriodoInscripcion::getAnioLectivo)
                .distinct()
                .flatMap(anioLectivo -> comisionesApi.materiasDictadas(planEstudioId, anioLectivo).stream()
                        .filter(materia -> abiertos.stream()
                                .anyMatch(periodo -> periodo.admite(anioLectivo, materia.periodoLectivo()))))
                .toList();
    }
}
