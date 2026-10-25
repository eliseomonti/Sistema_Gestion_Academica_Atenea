package com.utn.atenea.comisiones.service;

import com.utn.atenea.comisiones.dto.AulaResponse;
import com.utn.atenea.comisiones.mapper.AulaMapper;
import com.utn.atenea.comisiones.repository.AulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AulaService {

    private final AulaRepository aulaRepository;
    private final AulaMapper aulaMapper;

    public List<AulaResponse> listarActivas() {
        return aulaMapper.toResponse(aulaRepository.findByActivaTrueOrderByNombreAsc());
    }
}
