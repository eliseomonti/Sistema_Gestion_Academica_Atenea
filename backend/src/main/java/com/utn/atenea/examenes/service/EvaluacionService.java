package com.utn.atenea.examenes.service;

import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.dto.AltaEvaluacionRequest;
import com.utn.atenea.examenes.dto.EvaluacionResponse;
import com.utn.atenea.examenes.dto.ModificacionEvaluacionRequest;
import com.utn.atenea.examenes.model.EvaluacionCursada;
import com.utn.atenea.examenes.repository.EvaluacionCursadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Autoservicio del docente: las evaluaciones (parciales, TP, recuperatorios) de una materia que dicta.
 * docenteId es el personaId del token.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluacionService {

    private final EvaluacionCursadaRepository evaluacionCursadaRepository;
    private final AccesoDocente accesoDocente;

    /** Por fecha: el orden en que se toman. */
    public List<EvaluacionResponse> listar(Long docenteId, Long comisionMateriaId) {
        accesoDocente.validarCursada(docenteId, comisionMateriaId);
        return evaluacionCursadaRepository.findByComisionMateriaIdOrderByFechaAscIdAsc(comisionMateriaId).stream()
                .map(EvaluacionService::toResponse)
                .toList();
    }

    @Transactional
    public EvaluacionResponse alta(Long docenteId, Long comisionMateriaId, AltaEvaluacionRequest request) {
        accesoDocente.validarCursada(docenteId, comisionMateriaId);
        validarNombre(comisionMateriaId, request.nombre(), null);

        EvaluacionCursada evaluacion;
        if (request.evaluacionRecuperadaId() == null) {
            evaluacion = new EvaluacionCursada(comisionMateriaId, request.nombre(), request.fecha(), request.tipo());
        } else {
            EvaluacionCursada recuperada = evaluacionCursadaRepository.findById(request.evaluacionRecuperadaId())
                    .filter(otra -> otra.getComisionMateriaId().equals(comisionMateriaId))
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la evaluación con id "
                            + request.evaluacionRecuperadaId() + " en esta materia de la comisión"));
            validarFechaDeRecuperatorio(recuperada, request.fecha());
            evaluacion = recuperada.recuperatorio(request.nombre(), request.fecha());
        }
        return toResponse(evaluacionCursadaRepository.save(evaluacion));
    }

    @Transactional
    public EvaluacionResponse modificar(Long docenteId, Long evaluacionId, ModificacionEvaluacionRequest request) {
        EvaluacionCursada evaluacion = buscar(docenteId, evaluacionId);
        validarNombre(evaluacion.getComisionMateriaId(), request.nombre(), evaluacion.getId());
        if (evaluacion.esRecuperatorio()) {
            validarFechaDeRecuperatorio(evaluacion.getEvaluacionRecuperada(), request.fecha());
        }

        evaluacion.setNombre(request.nombre());
        evaluacion.setFecha(request.fecha());
        return toResponse(evaluacion);
    }

    /** Borrado real, para una evaluación mal cargada. Con notas o recuperatorios la FK RESTRICT lo impide. */
    @Transactional
    public void eliminar(Long docenteId, Long evaluacionId) {
        evaluacionCursadaRepository.delete(buscar(docenteId, evaluacionId));
        try {
            evaluacionCursadaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("La evaluación " + evaluacionId
                    + " tiene notas o recuperatorios: no se puede borrar");
        }
    }

    /** La evaluación, si es de una materia que dicta el docente. */
    EvaluacionCursada buscar(Long docenteId, Long evaluacionId) {
        EvaluacionCursada evaluacion = evaluacionCursadaRepository.findById(evaluacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la evaluación con id " + evaluacionId));
        accesoDocente.validarCursada(docenteId, evaluacion.getComisionMateriaId());
        return evaluacion;
    }

    // El UNIQUE (comision_materia_id, nombre) de la tabla.
    private void validarNombre(Long comisionMateriaId, String nombre, Long excluirEvaluacionId) {
        if (evaluacionCursadaRepository.nombreEnUso(comisionMateriaId, nombre, excluirEvaluacionId)) {
            throw new RecursoDuplicadoException("Ya hay una evaluación " + nombre + " en esta materia de la comisión");
        }
    }

    private static void validarFechaDeRecuperatorio(EvaluacionCursada recuperada, LocalDate fecha) {
        if (fecha.isBefore(recuperada.getFecha())) {
            throw new ReglaNegocioException("El recuperatorio no puede ser antes de " + recuperada.getNombre());
        }
    }

    private static EvaluacionResponse toResponse(EvaluacionCursada evaluacion) {
        return new EvaluacionResponse(evaluacion.getId(), evaluacion.getComisionMateriaId(), evaluacion.getNombre(),
                evaluacion.getTipo(), evaluacion.getFecha(),
                evaluacion.esRecuperatorio() ? evaluacion.getEvaluacionRecuperada().getId() : null);
    }
}
