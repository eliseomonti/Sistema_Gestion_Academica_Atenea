package com.utn.atenea.examenes.service;

import com.utn.atenea.examenes.ExamenFinalResumen;
import com.utn.atenea.examenes.ExamenesApi;
import com.utn.atenea.examenes.ResultadoCursada;
import com.utn.atenea.examenes.ResultadoFinal;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.examenes.repository.CierreCursadaRepository;
import com.utn.atenea.examenes.repository.ExamenFinalRepository;
import com.utn.atenea.examenes.repository.NotaFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamenesApiImpl implements ExamenesApi {

    private final CierreCursadaRepository cierreCursadaRepository;
    private final NotaFinalRepository notaFinalRepository;
    private final ExamenFinalRepository examenFinalRepository;

    @Override
    public List<ResultadoCursada> cierresVigentes(Collection<Long> inscripcionComisionIds) {
        if (inscripcionComisionIds.isEmpty()) {
            return List.of();
        }
        return cierreCursadaRepository.vigentes(inscripcionComisionIds).stream()
                .map(cierre -> new ResultadoCursada(cierre.getInscripcionComisionId(), cierre.getCondicion(),
                        cierre.getNotaCursada(), cierre.getFechaCierre(), cierre.getFechaVencimiento()))
                .toList();
    }

    @Override
    public List<ResultadoFinal> notasFinalesVigentes(Collection<Long> inscripcionExamenFinalIds) {
        if (inscripcionExamenFinalIds.isEmpty()) {
            return List.of();
        }
        return notaFinalRepository.vigentes(inscripcionExamenFinalIds).stream()
                .map(nota -> new ResultadoFinal(nota.getInscripcionExamenFinalId(), nota.getNota(), nota.estaAprobada()))
                .toList();
    }

    @Override
    public List<ExamenFinalResumen> buscarExamenesFinales(Collection<Long> examenFinalIds) {
        if (examenFinalIds.isEmpty()) {
            return List.of();
        }
        return examenFinalRepository.findByIdIn(examenFinalIds).stream()
                .map(ExamenesApiImpl::toResumen)
                .toList();
    }

    @Override
    public List<ExamenFinalResumen> examenesFinalesDeLosTurnos(Collection<Long> periodoIds) {
        if (periodoIds.isEmpty()) {
            return List.of();
        }
        return examenFinalRepository.findByPeriodoIdInOrderByFechaAscHoraInicioAsc(periodoIds).stream()
                .map(ExamenesApiImpl::toResumen)
                .toList();
    }

    private static ExamenFinalResumen toResumen(ExamenFinal mesa) {
        return new ExamenFinalResumen(mesa.getId(), mesa.getMateriaId(), mesa.getPeriodoId(), mesa.getFecha(),
                mesa.getHoraInicio(), mesa.getHoraFin(), mesa.getAulaId(), Set.copyOf(mesa.getDocenteIds()));
    }
}
