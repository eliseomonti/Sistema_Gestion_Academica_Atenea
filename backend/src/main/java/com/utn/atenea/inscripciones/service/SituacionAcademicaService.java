package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.comisiones.ComisionMateriaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.examenes.ExamenFinalResumen;
import com.utn.atenea.examenes.ExamenesApi;
import com.utn.atenea.examenes.ResultadoCursada;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import com.utn.atenea.inscripciones.model.InscripcionFinal;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica;
import com.utn.atenea.inscripciones.repository.InscripcionComisionRepository;
import com.utn.atenea.inscripciones.repository.InscripcionFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Junta lo que la situación académica necesita de cada módulo. Las reglas están en SituacionAcademica. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SituacionAcademicaService {

    private final InscripcionComisionRepository inscripcionComisionRepository;
    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final ComisionesApi comisionesApi;
    private final ExamenesApi examenesApi;
    private final CarrerasApi carrerasApi;

    /**
     * La situación en el plan de esa inscripción a carrera. Sólo cuenta lo hecho con esa inscripción: no
     * hay equivalencias, así que lo cursado en otra carrera no vale.
     */
    public SituacionAcademica de(Long inscripcionCarreraId, Long planEstudioId) {
        return new SituacionAcademica(carrerasApi.correlatividades(planEstudioId), cursadas(inscripcionCarreraId),
                finalesRendidos(inscripcionCarreraId), LocalDate.now());
    }

    private List<SituacionAcademica.Cursada> cursadas(Long inscripcionCarreraId) {
        List<InscripcionComision> inscripciones = inscripcionComisionRepository
                .findByInscripcionCarreraIdInAndEstado(List.of(inscripcionCarreraId), EstadoInscripcion.INSCRIPTO);
        if (inscripciones.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> materiaPorComisionMateria = comisionesApi
                .buscarComisionesMateria(inscripciones.stream().map(InscripcionComision::getComisionMateriaId).toList())
                .stream()
                .collect(Collectors.toMap(ComisionMateriaResumen::id, ComisionMateriaResumen::materiaId));
        Map<Long, ResultadoCursada> cierres = examenesApi
                .cierresVigentes(inscripciones.stream().map(InscripcionComision::getId).toList())
                .stream()
                .collect(Collectors.toMap(ResultadoCursada::inscripcionComisionId, Function.identity()));

        return inscripciones.stream()
                .map(inscripcion -> {
                    Long materiaId = materiaPorComisionMateria.get(inscripcion.getComisionMateriaId());
                    ResultadoCursada cierre = cierres.get(inscripcion.getId());
                    return cierre == null
                            ? new SituacionAcademica.Cursada(materiaId, null, null, null)
                            : new SituacionAcademica.Cursada(materiaId, cierre.condicion(), cierre.nota(),
                                    cierre.fechaVencimiento());
                })
                .toList();
    }

    private List<SituacionAcademica.FinalRendido> finalesRendidos(Long inscripcionCarreraId) {
        List<InscripcionFinal> inscripciones = inscripcionFinalRepository
                .findByInscripcionCarreraIdAndEstado(inscripcionCarreraId, EstadoInscripcion.INSCRIPTO);
        if (inscripciones.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> mesaPorInscripcion = inscripciones.stream()
                .collect(Collectors.toMap(InscripcionFinal::getId, InscripcionFinal::getExamenFinalId));
        Map<Long, Long> materiaPorMesa = examenesApi.buscarExamenesFinales(mesaPorInscripcion.values()).stream()
                .collect(Collectors.toMap(ExamenFinalResumen::id, ExamenFinalResumen::materiaId));

        return examenesApi.notasFinalesVigentes(mesaPorInscripcion.keySet()).stream()
                .map(nota -> new SituacionAcademica.FinalRendido(
                        materiaPorMesa.get(mesaPorInscripcion.get(nota.inscripcionExamenFinalId())), nota.nota(),
                        nota.aprobado()))
                .toList();
    }
}
