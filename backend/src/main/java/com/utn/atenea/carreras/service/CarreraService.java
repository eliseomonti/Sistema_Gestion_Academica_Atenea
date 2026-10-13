package com.utn.atenea.carreras.service;

import com.utn.atenea.carreras.dto.CarreraResponse;
import com.utn.atenea.carreras.mapper.CarreraMapper;
import com.utn.atenea.carreras.repository.CarreraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarreraService {

    private final CarreraRepository carreraRepository;
    private final CarreraMapper carreraMapper;

    public List<CarreraResponse> listar() {
        return carreraMapper.toResponse(carreraRepository.findAllByOrderByNombreAsc());
    }
}
