package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.examenes.ExamenFinalResumen;
import com.utn.atenea.examenes.ExamenesApi;
import com.utn.atenea.inscripciones.dto.InscripcionFinalRequest;
import com.utn.atenea.inscripciones.dto.InscripcionFinalResponse;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionFinal;
import com.utn.atenea.inscripciones.model.PeriodoInscripcion;
import com.utn.atenea.inscripciones.reglas.EstadoMateria;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica;
import com.utn.atenea.inscripciones.repository.InscripcionFinalRepository;
import com.utn.atenea.inscripciones.repository.PeriodoInscripcionRepository;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Autoservicio del estudiante: inscribirse a una mesa de examen final. estudianteId es el personaId del
 * token, nunca viene del request. Separado de InscripcionFinalService por la cantidad de dependencias:
 * es un solo caso de uso, pero valida contra casi todos los módulos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AltaInscripcionFinalService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final PeriodoInscripcionRepository periodoInscripcionRepository;
    private final ExamenesApi examenesApi;
    private final PersonasApi personasApi;
    private final CarrerasApi carrerasApi;
    private final SituacionAcademicaService situacionAcademicaService;
    private final InscripcionFinalService inscripcionFinalService;

    /**
     * Si el estudiante ya se había anotado y dado de baja de la misma mesa, se reactiva esa inscripción:
     * el UNIQUE de la tabla no deja crear otra. Los finales no tienen cupo: no hace falta bloquear la mesa.
     */
    @Transactional
    public InscripcionFinalResponse inscribir(Long estudianteId, InscripcionFinalRequest request) {
        ExamenFinalResumen mesa = examenesApi.buscarExamenesFinales(List.of(request.examenFinalId())).stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la mesa de examen con id " + request.examenFinalId()));
        // Una mesa sólo cuelga de un período de FINAL: lo garantiza la FK compuesta fk_final_periodo.
        PeriodoInscripcion turno = periodoInscripcionRepository.findById(mesa.periodoId()).orElseThrow();
        if (!turno.estaAbierto(LocalDateTime.now())) {
            throw new ReglaNegocioException("La inscripción al turno de esta mesa no está abierta");
        }

        InscripcionCarreraResumen inscripcionCarrera = inscripcionCarreraConLaMateria(
                estudianteId, turno.getCarreraId(), mesa.materiaId());
        InscripcionFinal existente = inscripcionFinalRepository
                .findByInscripcionCarreraIdAndExamenFinalId(inscripcionCarrera.id(), mesa.id())
                .orElse(null);
        if (existente != null && existente.getEstado() == EstadoInscripcion.INSCRIPTO) {
            throw new RecursoDuplicadoException("Ya estás inscripto a esta mesa");
        }
        validarNoInscriptoEnOtraMesa(inscripcionCarrera.id(), mesa);
        validarSituacionAcademica(inscripcionCarrera, mesa);

        LocalDate hoy = LocalDate.now();
        InscripcionFinal inscripcion;
        if (existente != null) {
            existente.reinscribir(hoy);
            inscripcion = existente;
        } else {
            inscripcion = inscripcionFinalRepository.save(
                    new InscripcionFinal(inscripcionCarrera.id(), mesa.id(), hoy));
        }
        return inscripcionFinalService.toResponses(List.of(inscripcion),
                Map.of(inscripcionCarrera.id(), inscripcionCarrera.planEstudioId())).getFirst();
    }

    /**
     * La inscripción activa a un plan de la carrera del turno que tenga la materia. Puede estar inscripto
     * a más de una carrera: el turno es de una sola.
     */
    private InscripcionCarreraResumen inscripcionCarreraConLaMateria(Long estudianteId, Long carreraId,
                                                                    Long materiaId) {
        return personasApi.inscripcionesCarrera(estudianteId).stream()
                .filter(InscripcionCarreraResumen::activa)
                .filter(inscripcion -> carrerasApi.buscarPlan(inscripcion.planEstudioId())
                        .map(PlanEstudioResumen::carreraId)
                        .filter(carreraId::equals)
                        .isPresent())
                .filter(inscripcion -> carrerasApi.buscarMateriaDelPlan(inscripcion.planEstudioId(), materiaId)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "No tenés una inscripción activa a un plan de estudio con esta materia"));
    }

    // En un turno, una materia se rinde en una sola mesa.
    private void validarNoInscriptoEnOtraMesa(Long inscripcionCarreraId, ExamenFinalResumen mesa) {
        List<Long> otrasMesas = examenesApi.examenesFinalesDeLosTurnos(List.of(mesa.periodoId())).stream()
                .filter(otra -> otra.materiaId().equals(mesa.materiaId()) && !otra.id().equals(mesa.id()))
                .map(ExamenFinalResumen::id)
                .toList();
        if (!otrasMesas.isEmpty() && inscripcionFinalRepository.existsByInscripcionCarreraIdAndExamenFinalIdInAndEstado(
                inscripcionCarreraId, otrasMesas, EstadoInscripcion.INSCRIPTO)) {
            throw new ReglaNegocioException("Ya estás inscripto a otra mesa de esta materia en el turno");
        }
    }

    // Las reglas son de SituacionAcademica; acá sólo se arma el mensaje.
    private void validarSituacionAcademica(InscripcionCarreraResumen inscripcionCarrera, ExamenFinalResumen mesa) {
        SituacionAcademica situacion = situacionAcademicaService.de(inscripcionCarrera.id(),
                inscripcionCarrera.planEstudioId());
        Long materiaId = mesa.materiaId();
        if (situacion.puedeRendir(materiaId, mesa.fecha())) {
            return;
        }
        EstadoMateria estado = situacion.estado(materiaId);
        if (estado == EstadoMateria.APROBADA) {
            throw new ReglaNegocioException("Ya tenés esta materia aprobada");
        }
        if (estado != EstadoMateria.REGULAR) {
            throw new ReglaNegocioException("Para rendir el final tenés que tener la materia regular");
        }
        LocalDate vencimiento = situacion.vencimientoDeLaRegularidad(materiaId).orElseThrow();
        if (vencimiento.isBefore(mesa.fecha())) {
            throw new ReglaNegocioException("Tu regularidad vence el " + vencimiento.format(FECHA)
                    + ", antes de la mesa");
        }
        Map<Long, String> nombres = carrerasApi.materiasDelPlan(inscripcionCarrera.planEstudioId()).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
        throw new ReglaNegocioException("Te faltan correlativas para rendir esta materia: "
                + situacion.faltanParaRendir(materiaId).stream().map(nombres::get).collect(Collectors.joining(", ")));
    }
}
