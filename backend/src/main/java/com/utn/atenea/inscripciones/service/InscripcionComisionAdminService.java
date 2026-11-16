package com.utn.atenea.inscripciones.service;

import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.ExamenesApi;
import com.utn.atenea.examenes.ResultadoCursada;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.inscripciones.dto.InscriptoComisionResponse;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import com.utn.atenea.inscripciones.repository.InscripcionComisionRepository;
import com.utn.atenea.personas.EstudianteResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InscripcionComisionAdminService {

    private final InscripcionComisionRepository inscripcionComisionRepository;
    private final PersonasApi personasApi;
    private final ExamenesApi examenesApi;

    public List<InscriptoComisionResponse> listar(Long comisionMateriaId) {
        List<InscripcionComision> inscripciones = inscripcionComisionRepository
                .findByComisionMateriaIdAndEstado(comisionMateriaId, EstadoInscripcion.INSCRIPTO);
        if (inscripciones.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> estudiantePorInscripcionCarrera = personasApi
                .buscarInscripcionesCarrera(inscripciones.stream()
                        .map(InscripcionComision::getInscripcionCarreraId).toList())
                .stream()
                .collect(Collectors.toMap(InscripcionCarreraResumen::id, InscripcionCarreraResumen::estudianteId));
        Map<Long, EstudianteResumen> estudiantes = personasApi
                .buscarEstudiantes(estudiantePorInscripcionCarrera.values())
                .stream()
                .collect(Collectors.toMap(EstudianteResumen::personaId, Function.identity()));

        return inscripciones.stream()
                .map(inscripcion -> {
                    EstudianteResumen estudiante = estudiantes.get(
                            estudiantePorInscripcionCarrera.get(inscripcion.getInscripcionCarreraId()));
                    return new InscriptoComisionResponse(inscripcion.getId(), estudiante.personaId(),
                            estudiante.legajoEstudiante(), estudiante.apellido(), estudiante.nombre(),
                            inscripcion.getFechaInscripcion());
                })
                .sorted(Comparator.comparing(InscriptoComisionResponse::apellido)
                        .thenComparing(InscriptoComisionResponse::nombre))
                .toList();
    }

    @Transactional
    public void darDeBaja(Long inscripcionId) {
        InscripcionComision inscripcion = inscripcionComisionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la inscripción a comisión con id " + inscripcionId));
        if (inscripcion.getEstado() == EstadoInscripcion.BAJA) {
            throw new ReglaNegocioException("La inscripción ya está dada de baja");
        }
        List<ResultadoCursada> cierre = examenesApi.cierresVigentes(List.of(inscripcionId));
        if (!cierre.isEmpty()) {
            throw new ReglaNegocioException("La cursada ya tiene acta de cierre ("
                    + cierre.getFirst().condicion() + "): no se puede dar de baja");
        }

        inscripcion.darDeBaja(LocalDate.now());
    }

}
