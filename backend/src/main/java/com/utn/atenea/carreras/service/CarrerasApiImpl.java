package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarrerasApiImpl implements CarrerasApi {

    private final PlanEstudioRepository planEstudioRepository;

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
}
