package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.dto.PlanMateriaResponse;
import com.utn.atenea.carreras.mapper.PlanMateriaMapper;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import com.utn.atenea.carreras.repository.PlanMateriaRepository;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanMateriaService {

    private final PlanEstudioRepository planEstudioRepository;
    private final PlanMateriaRepository planMateriaRepository;
    private final PlanMateriaMapper planMateriaMapper;

    /** Las materias del plan, de un año de la carrera o (con anioCursado null) de todos. */
    public List<PlanMateriaResponse> listar(Long carreraId, Long planEstudioId, Integer anioCursado) {
        // Un plan de otra carrera tampoco se encuentra: la URL tiene que ser coherente.
        if (!planEstudioRepository.existsByIdAndCarreraId(planEstudioId, carreraId)) {
            throw new RecursoNoEncontradoException(
                    "No existe el plan con id " + planEstudioId + " en la carrera con id " + carreraId);
        }
        return planMateriaMapper.toResponse(planMateriaRepository.buscarDelPlan(planEstudioId, anioCursado));
    }
}
