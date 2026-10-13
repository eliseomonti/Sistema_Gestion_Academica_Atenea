package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.dto.PlanEstudioResponse;
import com.utn.atenea.carreras.mapper.PlanEstudioMapper;
import com.utn.atenea.carreras.repository.CarreraRepository;
import com.utn.atenea.carreras.repository.PlanEstudioRepository;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanEstudioService {

    private final PlanEstudioRepository planEstudioRepository;
    private final CarreraRepository carreraRepository;
    private final PlanEstudioMapper planEstudioMapper;

    public List<PlanEstudioResponse> listarPorCarrera(Long carreraId) {
        if (!carreraRepository.existsById(carreraId)) {
            throw new RecursoNoEncontradoException("No existe la carrera con id " + carreraId);
        }
        return planEstudioMapper.toResponse(planEstudioRepository.findByCarreraIdOrderByAnioInicioDesc(carreraId));
    }
}
