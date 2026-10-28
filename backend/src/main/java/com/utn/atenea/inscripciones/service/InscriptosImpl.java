package com.utn.atenea.inscripciones.service;

import com.utn.atenea.examenes.InscriptoACursada;
import com.utn.atenea.examenes.InscriptoAFinal;
import com.utn.atenea.examenes.Inscriptos;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import com.utn.atenea.inscripciones.model.InscripcionFinal;
import com.utn.atenea.inscripciones.repository.InscripcionComisionRepository;
import com.utn.atenea.inscripciones.repository.InscripcionFinalRepository;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Implementa el puerto que declara examenes: así examenes no depende de inscripciones. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InscriptosImpl implements Inscriptos {

    private final InscripcionComisionRepository inscripcionComisionRepository;
    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final PersonasApi personasApi;

    @Override
    public List<InscriptoACursada> cursadasDelEstudiante(Long estudianteId) {
        Map<Long, Long> planPorInscripcionCarrera = planPorInscripcionCarrera(estudianteId);
        if (planPorInscripcionCarrera.isEmpty()) {
            return List.of();
        }
        return inscripcionComisionRepository
                .findByInscripcionCarreraIdInAndEstado(planPorInscripcionCarrera.keySet(), EstadoInscripcion.INSCRIPTO)
                .stream()
                .map(inscripcion -> new InscriptoACursada(inscripcion.getId(), inscripcion.getComisionMateriaId(),
                        estudianteId))
                .toList();
    }

    @Override
    public List<InscriptoAFinal> finalesDelEstudiante(Long estudianteId) {
        Map<Long, Long> planPorInscripcionCarrera = planPorInscripcionCarrera(estudianteId);
        if (planPorInscripcionCarrera.isEmpty()) {
            return List.of();
        }
        return inscripcionFinalRepository
                .findByInscripcionCarreraIdInAndEstado(planPorInscripcionCarrera.keySet(), EstadoInscripcion.INSCRIPTO)
                .stream()
                .map(inscripcion -> new InscriptoAFinal(inscripcion.getId(), inscripcion.getExamenFinalId(),
                        planPorInscripcionCarrera.get(inscripcion.getInscripcionCarreraId()), estudianteId))
                .toList();
    }

    @Override
    public List<InscriptoACursada> inscriptosALaCursada(Long comisionMateriaId) {
        List<InscripcionComision> inscripciones = inscripcionComisionRepository
                .findByComisionMateriaIdAndEstado(comisionMateriaId, EstadoInscripcion.INSCRIPTO);
        Map<Long, InscripcionCarreraResumen> inscripcionesCarrera = inscripcionesCarrera(
                inscripciones.stream().map(InscripcionComision::getInscripcionCarreraId).toList());
        return inscripciones.stream()
                .map(inscripcion -> new InscriptoACursada(inscripcion.getId(), comisionMateriaId,
                        inscripcionesCarrera.get(inscripcion.getInscripcionCarreraId()).estudianteId()))
                .toList();
    }

    @Override
    public List<InscriptoAFinal> inscriptosALaMesa(Long examenFinalId) {
        List<InscripcionFinal> inscripciones = inscripcionFinalRepository
                .findByExamenFinalIdAndEstado(examenFinalId, EstadoInscripcion.INSCRIPTO);
        Map<Long, InscripcionCarreraResumen> inscripcionesCarrera = inscripcionesCarrera(
                inscripciones.stream().map(InscripcionFinal::getInscripcionCarreraId).toList());
        return inscripciones.stream()
                .map(inscripcion -> {
                    InscripcionCarreraResumen inscripcionCarrera = inscripcionesCarrera.get(
                            inscripcion.getInscripcionCarreraId());
                    return new InscriptoAFinal(inscripcion.getId(), examenFinalId, inscripcionCarrera.planEstudioId(),
                            inscripcionCarrera.estudianteId());
                })
                .toList();
    }

    private Map<Long, InscripcionCarreraResumen> inscripcionesCarrera(List<Long> inscripcionCarreraIds) {
        return personasApi.buscarInscripcionesCarrera(inscripcionCarreraIds).stream()
                .collect(Collectors.toMap(InscripcionCarreraResumen::id, Function.identity()));
    }

    // Todas las inscripciones a carrera, también las inactivas: las notas son de lo ya hecho.
    private Map<Long, Long> planPorInscripcionCarrera(Long estudianteId) {
        return personasApi.inscripcionesCarrera(estudianteId).stream()
                .collect(Collectors.toMap(InscripcionCarreraResumen::id, InscripcionCarreraResumen::planEstudioId));
    }
}
