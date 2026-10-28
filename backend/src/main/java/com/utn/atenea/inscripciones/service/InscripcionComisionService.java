package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.comisiones.ComisionMateriaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.inscripciones.dto.InscripcionComisionResponse;
import com.utn.atenea.inscripciones.mapper.InscripcionComisionMapper;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import com.utn.atenea.inscripciones.repository.InscripcionComisionRepository;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Autoservicio del estudiante: ver sus inscripciones a comisión y darse de baja. La inscripción está en
 * AltaInscripcionComisionService. estudianteId es el personaId del token, nunca viene del request.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InscripcionComisionService {

    private final InscripcionComisionRepository inscripcionComisionRepository;
    private final ComisionesApi comisionesApi;
    private final PersonasApi personasApi;
    private final CarrerasApi carrerasApi;
    private final InscripcionComisionMapper inscripcionComisionMapper;

    /** Las inscripciones vigentes del estudiante en ese año lectivo (sin año, el actual), por período y materia. */
    public List<InscripcionComisionResponse> listar(Long estudianteId, Integer anioLectivo) {
        int anio = anioLectivo != null ? anioLectivo : LocalDate.now().getYear();
        List<Long> inscripcionCarreraIds = personasApi.inscripcionesCarrera(estudianteId).stream()
                .map(InscripcionCarreraResumen::id)
                .toList();
        if (inscripcionCarreraIds.isEmpty()) {
            return List.of();
        }
        List<InscripcionComision> inscripciones = inscripcionComisionRepository
                .findByInscripcionCarreraIdInAndEstado(inscripcionCarreraIds, EstadoInscripcion.INSCRIPTO);

        // La comisión de cada inscripción y los nombres de las materias se piden una sola vez, para todas.
        Map<Long, ComisionMateriaResumen> comisionesMateria = comisionesApi
                .buscarComisionesMateria(inscripciones.stream().map(InscripcionComision::getComisionMateriaId).toList())
                .stream()
                .filter(comisionMateria -> comisionMateria.anioLectivo() == anio)
                .collect(Collectors.toMap(ComisionMateriaResumen::id, Function.identity()));
        Map<Long, Map<Long, String>> nombresPorPlan = new HashMap<>();

        return inscripciones.stream()
                .filter(inscripcion -> comisionesMateria.containsKey(inscripcion.getComisionMateriaId()))
                .map(inscripcion -> {
                    ComisionMateriaResumen comisionMateria = comisionesMateria.get(inscripcion.getComisionMateriaId());
                    String nombreMateria = nombresPorPlan
                            .computeIfAbsent(comisionMateria.planEstudioId(), this::nombresDeMaterias)
                            .get(comisionMateria.materiaId());
                    return inscripcionComisionMapper.toResponse(inscripcion, comisionMateria, nombreMateria);
                })
                .sorted(Comparator.comparing(InscripcionComisionResponse::periodoLectivo)
                        .thenComparing(InscripcionComisionResponse::nombreMateria))
                .toList();
    }

    /** El estudiante se puede dar de baja mientras el período en que se anotó siga abierto. */
    @Transactional
    public InscripcionComisionResponse darDeBaja(Long estudianteId, Long inscripcionId) {
        List<Long> inscripcionCarreraIds = personasApi.inscripcionesCarrera(estudianteId).stream()
                .map(InscripcionCarreraResumen::id)
                .toList();
        // Con las inscripciones a carrera del estudiante en la condición, la de otro da 404 y no 403:
        // no se revela que existe.
        InscripcionComision inscripcion = inscripcionComisionRepository
                .findByIdAndInscripcionCarreraIdIn(inscripcionId, inscripcionCarreraIds)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la inscripción a comisión con id " + inscripcionId));
        if (inscripcion.getEstado() == EstadoInscripcion.BAJA) {
            throw new ReglaNegocioException("La inscripción a comisión " + inscripcionId + " ya está dada de baja");
        }
        if (!inscripcion.getPeriodo().estaAbierto(LocalDateTime.now())) {
            throw new ReglaNegocioException(
                    "El período de inscripción ya cerró: la baja de la cursada se pide en la institución");
        }

        inscripcion.darDeBaja(LocalDate.now());
        ComisionMateriaResumen comisionMateria = comisionesApi
                .buscarComisionesMateria(List.of(inscripcion.getComisionMateriaId())).getFirst();
        return toResponse(inscripcion, comisionMateria);
    }

    private InscripcionComisionResponse toResponse(InscripcionComision inscripcion,
                                                   ComisionMateriaResumen comisionMateria) {
        String nombreMateria = carrerasApi.buscarMateriaDelPlan(comisionMateria.planEstudioId(),
                        comisionMateria.materiaId())
                .map(MateriaDelPlan::nombre)
                .orElse(null);
        return inscripcionComisionMapper.toResponse(inscripcion, comisionMateria, nombreMateria);
    }

    private Map<Long, String> nombresDeMaterias(Long planEstudioId) {
        return carrerasApi.materiasDelPlan(planEstudioId).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
    }
}
