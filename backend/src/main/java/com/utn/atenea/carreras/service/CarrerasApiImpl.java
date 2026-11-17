package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.CorrelativaDelPlan;
import com.utn.atenea.carreras.CorrelativasDeMateria;
import com.utn.atenea.carreras.EstadoCorrelativa;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.MateriaResumen;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.carreras.model.Correlatividad;
import com.utn.atenea.carreras.model.PlanMateria;
import com.utn.atenea.carreras.repository.CarreraRepository;
import com.utn.atenea.carreras.repository.CorrelatividadRepository;
import com.utn.atenea.carreras.repository.MateriaRepository;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import com.utn.atenea.carreras.repository.PlanMateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarrerasApiImpl implements CarrerasApi {

    /**
     * Los códigos de un plan tienen todos el mismo formato ("01", "77-10-101") o son números sin
     * ceros adelante ("1", "10"). Comparar primero el largo hace que "5" vaya antes que "10".
     */
    private static final Comparator<String> POR_CODIGO = Comparator.nullsLast(
            Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));

    private final CarreraRepository carreraRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final PlanMateriaRepository planMateriaRepository;
    private final CorrelatividadRepository correlatividadRepository;
    private final MateriaRepository materiaRepository;

    @Override
    public boolean existeCarrera(Long carreraId) {
        return carreraRepository.existsById(carreraId);
    }

    @Override
    public Optional<PlanEstudioResumen> buscarPlan(Long planEstudioId) {
        return planEstudioRepository.findById(planEstudioId)
                .map(plan -> new PlanEstudioResumen(plan.getId(), plan.getCarrera().getId(),
                        plan.getCarrera().getNombre(), plan.getNombre(), plan.admiteInscripciones()));
    }

    @Override
    public List<Long> idsDePlanes(Long carreraId) {
        return planEstudioRepository.findIdsByCarreraId(carreraId);
    }

    @Override
    public int cantidadDeAnios(Long planEstudioId) {
        return planMateriaRepository.cantidadDeAnios(planEstudioId);
    }

    @Override
    public Optional<MateriaDelPlan> buscarMateriaDelPlan(Long planEstudioId, Long materiaId) {
        return planMateriaRepository.findByPlanEstudioIdAndMateriaId(planEstudioId, materiaId)
                .map(CarrerasApiImpl::toMateriaDelPlan);
    }

    @Override
    public List<MateriaDelPlan> materiasDelPlan(Long planEstudioId) {
        return planMateriaRepository.buscarDelPlan(planEstudioId, null).stream()
                .map(CarrerasApiImpl::toMateriaDelPlan)
                .toList();
    }

    @Override
    public List<MateriaResumen> buscarMaterias(Collection<Long> materiaIds) {
        return materiaRepository.findAllById(materiaIds).stream()
                .map(materia -> new MateriaResumen(materia.getId(), materia.getNombre()))
                .toList();
    }

    @Override
    public List<CorrelativaDelPlan> correlatividades(Long planEstudioId) {
        return correlatividadRepository.buscarDelPlan(planEstudioId).stream()
                .map(correlatividad -> new CorrelativaDelPlan(
                        correlatividad.getPlanMateria().getMateria().getId(),
                        correlatividad.getCorrelativa().getMateria().getId(),
                        correlatividad.getEstadoParaCursar(),
                        correlatividad.getEstadoParaRendir()))
                .toList();
    }

    @Override
    public List<CorrelativasDeMateria> regimenDeCorrelatividades(Long planEstudioId) {
        Map<Long, List<Correlatividad>> porPlanMateria = correlatividadRepository.buscarDelPlan(planEstudioId).stream()
                .collect(Collectors.groupingBy(correlatividad -> correlatividad.getPlanMateria().getId()));
        return planMateriaRepository.buscarDelPlan(planEstudioId, null).stream()
                .sorted(Comparator.comparingInt(PlanMateria::getAnioCursado)
                        .thenComparing(PlanMateria::getCodigo, POR_CODIGO))
                .map(planMateria -> {
                    List<Correlatividad> correlativas = porPlanMateria.getOrDefault(planMateria.getId(), List.of());
                    return new CorrelativasDeMateria(planMateria.getMateria().getId(), planMateria.getCodigo(),
                            planMateria.getMateria().getNombre(), planMateria.getAnioCursado(),
                            planMateria.getPeriodoLectivo(),
                            codigos(correlativas, Correlatividad::getEstadoParaCursar, EstadoCorrelativa.CURSADA),
                            codigos(correlativas, Correlatividad::getEstadoParaCursar, EstadoCorrelativa.APROBADA),
                            codigos(correlativas, Correlatividad::getEstadoParaRendir, EstadoCorrelativa.CURSADA),
                            codigos(correlativas, Correlatividad::getEstadoParaRendir, EstadoCorrelativa.APROBADA));
                })
                .toList();
    }

    private static List<String> codigos(List<Correlatividad> correlativas,
                                        Function<Correlatividad, EstadoCorrelativa> exigido,
                                        EstadoCorrelativa estado) {
        return correlativas.stream()
                .filter(correlatividad -> exigido.apply(correlatividad) == estado)
                .map(correlatividad -> correlatividad.getCorrelativa().getCodigo())
                .sorted(POR_CODIGO)
                .toList();
    }

    private static MateriaDelPlan toMateriaDelPlan(PlanMateria planMateria) {
        return new MateriaDelPlan(planMateria.getMateria().getId(), planMateria.getMateria().getNombre(),
                planMateria.getAnioCursado(), planMateria.getPeriodoLectivo());
    }
}
