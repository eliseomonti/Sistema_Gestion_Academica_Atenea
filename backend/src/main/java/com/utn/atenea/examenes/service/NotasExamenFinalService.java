package com.utn.atenea.examenes.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaResumen;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.InscriptoAFinal;
import com.utn.atenea.examenes.Inscriptos;
import com.utn.atenea.examenes.dto.CargaNotasRequest;
import com.utn.atenea.examenes.dto.NotaEstudianteResponse;
import com.utn.atenea.examenes.dto.NotaRequest;
import com.utn.atenea.examenes.dto.NotasExamenFinalResponse;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.examenes.model.NotaFinal;
import com.utn.atenea.examenes.repository.ExamenFinalRepository;
import com.utn.atenea.examenes.repository.NotaFinalRepository;
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

/**
 * Autoservicio del docente: ver y cargar las notas de una mesa de cuyo tribunal es parte. docenteId es el
 * personaId del token.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotasExamenFinalService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExamenFinalRepository examenFinalRepository;
    private final NotaFinalRepository notaFinalRepository;
    private final AccesoDocente accesoDocente;
    private final Inscriptos inscriptos;
    private final PersonasApi personasApi;
    private final CarrerasApi carrerasApi;

    public NotasExamenFinalResponse planilla(Long docenteId, Long examenFinalId) {
        ExamenFinal mesa = buscar(docenteId, examenFinalId);
        return planilla(mesa, inscriptos.inscriptosALaMesa(mesa.getId()));
    }

    /**
     * Sin nota vigente se agrega la original; con una distinta, se rectifica; con la misma, no se toca. Las
     * notas no se modifican nunca (ver NotaFinal): corregir deja el historial.
     */
    @Transactional
    public NotasExamenFinalResponse cargar(Long docenteId, Long examenFinalId, CargaNotasRequest request) {
        ExamenFinal mesa = buscar(docenteId, examenFinalId);
        LocalDate hoy = LocalDate.now();
        if (mesa.getFecha().isAfter(hoy)) {
            throw new ReglaNegocioException("La mesa todavía no se tomó: es el " + mesa.getFecha().format(FECHA));
        }
        List<InscriptoAFinal> anotados = inscriptos.inscriptosALaMesa(mesa.getId());
        Planilla.validarInscripciones(request.notas().stream().map(NotaRequest::inscripcionId).toList(),
                anotados.stream().map(InscriptoAFinal::inscripcionExamenFinalId).collect(Collectors.toSet()));

        Map<Long, NotaFinal> vigentes = vigentes(anotados);
        for (NotaRequest nota : request.notas()) {
            NotaFinal vigente = vigentes.get(nota.inscripcionId());
            if (vigente == null) {
                notaFinalRepository.save(new NotaFinal(nota.inscripcionId(), docenteId, nota.nota(), hoy));
            } else if (!Objects.equals(vigente.getNota(), nota.nota())) {
                notaFinalRepository.save(vigente.rectificar(docenteId, nota.nota(), hoy));
            }
        }
        return planilla(mesa, anotados);
    }

    private ExamenFinal buscar(Long docenteId, Long examenFinalId) {
        ExamenFinal mesa = examenFinalRepository.findById(examenFinalId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la mesa de examen con id " + examenFinalId));
        accesoDocente.validarMesa(docenteId, mesa);
        return mesa;
    }

    private NotasExamenFinalResponse planilla(ExamenFinal mesa, List<InscriptoAFinal> anotados) {
        Map<Long, EstudianteResumen> estudiantes = personasApi
                .buscarEstudiantes(anotados.stream().map(InscriptoAFinal::estudianteId).toList()).stream()
                .collect(Collectors.toMap(EstudianteResumen::personaId, Function.identity()));
        Map<Long, NotaFinal> vigentes = vigentes(anotados);
        String nombreMateria = carrerasApi.buscarMaterias(List.of(mesa.getMateriaId())).stream()
                .findFirst()
                .map(MateriaResumen::nombre)
                .orElse(null);

        List<NotaEstudianteResponse> notas = anotados.stream()
                .map(anotado -> {
                    NotaFinal vigente = vigentes.get(anotado.inscripcionExamenFinalId());
                    return Planilla.fila(anotado.inscripcionExamenFinalId(), estudiantes.get(anotado.estudianteId()),
                            vigente != null ? vigente.getCondicion() : null,
                            vigente != null ? vigente.getNota() : null);
                })
                .sorted(Planilla.POR_APELLIDO)
                .toList();
        return new NotasExamenFinalResponse(mesa.getId(), mesa.getMateriaId(), nombreMateria, mesa.getFecha(), notas);
    }

    private Map<Long, NotaFinal> vigentes(List<InscriptoAFinal> anotados) {
        if (anotados.isEmpty()) {
            return Map.of();
        }
        return notaFinalRepository.vigentes(anotados.stream().map(InscriptoAFinal::inscripcionExamenFinalId).toList())
                .stream()
                .collect(Collectors.toMap(NotaFinal::getInscripcionExamenFinalId, Function.identity()));
    }
}
