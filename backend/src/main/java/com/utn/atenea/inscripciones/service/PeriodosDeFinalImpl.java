package com.utn.atenea.inscripciones.service;

import com.utn.atenea.examenes.PeriodoDeFinal;
import com.utn.atenea.examenes.PeriodosDeFinal;
import com.utn.atenea.inscripciones.model.TipoPeriodo;
import com.utn.atenea.inscripciones.repository.PeriodoInscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Implementa el puerto que declara examenes: así examenes no depende de inscripciones. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeriodosDeFinalImpl implements PeriodosDeFinal {

    private final PeriodoInscripcionRepository periodoInscripcionRepository;

    @Override
    public Optional<PeriodoDeFinal> buscar(Long periodoId) {
        return periodoInscripcionRepository.findById(periodoId)
                .filter(periodo -> periodo.getTipo() == TipoPeriodo.FINAL)
                .map(periodo -> new PeriodoDeFinal(periodo.getId(), periodo.getCarreraId(), periodo.getAnioLectivo(),
                        periodo.getFechaInicio(), periodo.getFechaCierre()));
    }
}
