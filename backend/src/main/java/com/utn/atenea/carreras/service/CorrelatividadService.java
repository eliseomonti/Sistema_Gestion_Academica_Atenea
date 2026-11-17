package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.dto.CorrelatividadesMateriaResponse;
import com.utn.atenea.carreras.mapper.CorrelatividadMapper;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorrelatividadService {

    private final PlanEstudioRepository planEstudioRepository;
    private final CarrerasApi carrerasApi;
    private final CorrelatividadMapper correlatividadMapper;

    /** El régimen de correlatividades del plan: todas sus materias, cada una con sus correlativas. */
    public List<CorrelatividadesMateriaResponse> delPlan(Long carreraId, Long planEstudioId) {
        // Un plan de otra carrera tampoco se encuentra: la URL tiene que ser coherente.
        if (!planEstudioRepository.existsByIdAndCarreraId(planEstudioId, carreraId)) {
            throw new RecursoNoEncontradoException(
                    "No existe el plan con id " + planEstudioId + " en la carrera con id " + carreraId);
        }
        return correlatividadMapper.toResponse(carrerasApi.regimenDeCorrelatividades(planEstudioId));
    }
}
