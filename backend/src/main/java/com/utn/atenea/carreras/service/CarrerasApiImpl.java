package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.carreras.model.PlanMateria;
import com.utn.atenea.carreras.repository.CarreraRepository;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import com.utn.atenea.carreras.repository.PlanMateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarrerasApiImpl implements CarrerasApi {

    private final CarreraRepository carreraRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final PlanMateriaRepository planMateriaRepository;

    @Override
    public boolean existeCarrera(Long carreraId) {
        return carreraRepository.existsById(carreraId);
    }

    @Override
    public Optional<PlanEstudioResumen> buscarPlan(Long planEstudioId) {
        return planEstudioRepository.findById(planEstudioId)
                .map(plan -> new PlanEstudioResumen(
                        plan.getId(), plan.getCarrera().getId(), plan.admiteInscripciones()));
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

    private static MateriaDelPlan toMateriaDelPlan(PlanMateria planMateria) {
        return new MateriaDelPlan(planMateria.getMateria().getId(), planMateria.getMateria().getNombre(),
                planMateria.getAnioCursado(), planMateria.getPeriodoLectivo());
    }
}
