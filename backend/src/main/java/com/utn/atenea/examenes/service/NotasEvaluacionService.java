package com.utn.atenea.examenes.service;

import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.InscriptoACursada;
import com.utn.atenea.examenes.Inscriptos;
import com.utn.atenea.examenes.dto.CargaNotasRequest;
import com.utn.atenea.examenes.dto.NotaEstudianteResponse;
import com.utn.atenea.examenes.dto.NotaRequest;
import com.utn.atenea.examenes.dto.NotasEvaluacionResponse;
import com.utn.atenea.examenes.model.EvaluacionCursada;
import com.utn.atenea.examenes.model.NotaEvaluacion;
import com.utn.atenea.examenes.repository.CierreCursadaRepository;
import com.utn.atenea.examenes.repository.NotaEvaluacionRepository;
import com.utn.atenea.personas.EstudianteResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Autoservicio del docente: ver y cargar las notas de una evaluación. docenteId es el personaId del token. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotasEvaluacionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EvaluacionService evaluacionService;
    private final NotaEvaluacionRepository notaEvaluacionRepository;
    private final CierreCursadaRepository cierreCursadaRepository;
    private final Inscriptos inscriptos;
    private final PersonasApi personasApi;

    public NotasEvaluacionResponse planilla(Long docenteId, Long evaluacionId) {
        EvaluacionCursada evaluacion = evaluacionService.buscar(docenteId, evaluacionId);
        return planilla(evaluacion, inscriptos.inscriptosALaCursada(evaluacion.getComisionMateriaId()));
    }

    /**
     * Sin nota vigente se agrega la original; con una distinta, se rectifica; con la misma, no se toca. Las
     * notas no se modifican nunca (ver NotaEvaluacion): corregir deja el historial. Una cursada con acta de
     * cierre ya no recibe notas.
     */
    @Transactional
    public NotasEvaluacionResponse cargar(Long docenteId, Long evaluacionId, CargaNotasRequest request) {
        EvaluacionCursada evaluacion = evaluacionService.buscar(docenteId, evaluacionId);
        LocalDate hoy = LocalDate.now();
        if (evaluacion.getFecha().isAfter(hoy)) {
            throw new ReglaNegocioException("La evaluación todavía no se tomó: es el "
                    + evaluacion.getFecha().format(FECHA));
        }
        List<InscriptoACursada> anotados = inscriptos.inscriptosALaCursada(evaluacion.getComisionMateriaId());
        List<Long> inscripcionIds = request.notas().stream().map(NotaRequest::inscripcionId).toList();
        Planilla.validarInscripciones(inscripcionIds,
                anotados.stream().map(InscriptoACursada::inscripcionComisionId).collect(Collectors.toSet()));
        validarSinActa(inscripcionIds);

        Map<Long, NotaEvaluacion> vigentes = vigentes(evaluacion.getId());
        for (NotaRequest nota : request.notas()) {
            NotaEvaluacion vigente = vigentes.get(nota.inscripcionId());
            if (vigente == null) {
                notaEvaluacionRepository.save(
                        new NotaEvaluacion(evaluacion, nota.inscripcionId(), docenteId, nota.nota(), hoy));
            } else if (!Objects.equals(vigente.getNota(), nota.nota())) {
                notaEvaluacionRepository.save(vigente.rectificar(docenteId, nota.nota(), hoy));
            }
        }
        return planilla(evaluacion, anotados);
    }

    // Con el acta de cierre, las notas de la cursada quedan como estaban: lo que cuenta es el acta.
    private void validarSinActa(List<Long> inscripcionIds) {
        cierreCursadaRepository.vigentes(inscripcionIds).stream().findFirst().ifPresent(cierre -> {
            throw new ReglaNegocioException("La cursada de la inscripción " + cierre.getInscripcionComisionId()
                    + " ya tiene acta de cierre: sus notas no se cambian");
        });
    }

    private NotasEvaluacionResponse planilla(EvaluacionCursada evaluacion, List<InscriptoACursada> anotados) {
        Map<Long, EstudianteResumen> estudiantes = personasApi
                .buscarEstudiantes(anotados.stream().map(InscriptoACursada::estudianteId).toList()).stream()
                .collect(Collectors.toMap(EstudianteResumen::personaId, Function.identity()));
        Map<Long, NotaEvaluacion> vigentes = vigentes(evaluacion.getId());

        List<NotaEstudianteResponse> notas = anotados.stream()
                .map(anotado -> {
                    NotaEvaluacion vigente = vigentes.get(anotado.inscripcionComisionId());
                    return Planilla.fila(anotado.inscripcionComisionId(), estudiantes.get(anotado.estudianteId()),
                            vigente != null ? vigente.getCondicion() : null,
                            vigente != null ? vigente.getNota() : null);
                })
                .sorted(Planilla.POR_APELLIDO)
                .toList();
        return new NotasEvaluacionResponse(evaluacion.getId(), evaluacion.getNombre(), evaluacion.getTipo(),
                evaluacion.getFecha(), notas);
    }

    private Map<Long, NotaEvaluacion> vigentes(Long evaluacionId) {
        return notaEvaluacionRepository.vigentesDeLaEvaluacion(evaluacionId).stream()
                .collect(Collectors.toMap(NotaEvaluacion::getInscripcionComisionId, Function.identity()));
    }
}
