package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.CorrelativaDelPlan;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.MateriaResumen;
import com.utn.atenea.carreras.PlanEstudioResumen;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarrerasApiImpl implements CarrerasApi {

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

    private static MateriaDelPlan toMateriaDelPlan(PlanMateria planMateria) {
        return new MateriaDelPlan(planMateria.getMateria().getId(), planMateria.getMateria().getNombre(),
                planMateria.getAnioCursado(), planMateria.getPeriodoLectivo());
    }
}
