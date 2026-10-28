package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.PlanEstudioResumen;
import com.utn.atenea.comisiones.ComisionMateriaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.inscripciones.dto.InscripcionComisionRequest;
import com.utn.atenea.inscripciones.dto.InscripcionComisionResponse;
import com.utn.atenea.inscripciones.mapper.InscripcionComisionMapper;
import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import com.utn.atenea.inscripciones.model.PeriodoInscripcion;
import com.utn.atenea.inscripciones.model.TipoPeriodo;
import com.utn.atenea.inscripciones.reglas.EstadoMateria;
import com.utn.atenea.inscripciones.reglas.SituacionAcademica;
import com.utn.atenea.inscripciones.repository.InscripcionComisionRepository;
import com.utn.atenea.inscripciones.repository.PeriodoInscripcionRepository;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Autoservicio del estudiante: inscribirse a una materia de una comisión. estudianteId es el personaId
 * del token, nunca viene del request. Separado de InscripcionComisionService por la cantidad de
 * dependencias: es un solo caso de uso, pero valida contra casi todos los módulos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AltaInscripcionComisionService {

    private final InscripcionComisionRepository inscripcionComisionRepository;
    private final PeriodoInscripcionRepository periodoInscripcionRepository;
    private final ComisionesApi comisionesApi;
    private final PersonasApi personasApi;
    private final CarrerasApi carrerasApi;
    private final SituacionAcademicaService situacionAcademicaService;
    private final InscripcionComisionMapper inscripcionComisionMapper;

    /**
     * Si el estudiante ya se había anotado y dado de baja de la misma materia de la comisión, se
     * reactiva esa inscripción: el UNIQUE de la tabla no deja crear otra.
     */
    @Transactional
    public InscripcionComisionResponse inscribir(Long estudianteId, InscripcionComisionRequest request) {
        // Primero el bloqueo: desde acá hasta el commit nadie más se inscribe a esta materia de la comisión.
        ComisionMateriaResumen comisionMateria = comisionesApi.bloquearComisionMateria(request.comisionMateriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la materia de comisión con id " + request.comisionMateriaId()));
        if (!comisionMateria.comisionActiva()) {
            throw new ReglaNegocioException("La comisión " + comisionMateria.nombreComision() + " está dada de baja");
        }

        Long inscripcionCarreraId = inscripcionCarreraActiva(estudianteId, comisionMateria.planEstudioId());
        PeriodoInscripcion periodo = periodoAbierto(comisionMateria);

        InscripcionComision existente = inscripcionComisionRepository
                .findByInscripcionCarreraIdAndComisionMateriaId(inscripcionCarreraId, comisionMateria.id())
                .orElse(null);
        if (existente != null && existente.getEstado() == EstadoInscripcion.INSCRIPTO) {
            throw new RecursoDuplicadoException("Ya estás inscripto a esta materia en la comisión "
                    + comisionMateria.nombreComision());
        }
        validarNoInscriptoEnOtraComision(inscripcionCarreraId, comisionMateria);
        validarSituacionAcademica(inscripcionCarreraId, comisionMateria);
        validarCupo(comisionMateria);

        LocalDate hoy = LocalDate.now();
        InscripcionComision inscripcion;
        if (existente != null) {
            existente.reinscribir(periodo, hoy);
            inscripcion = existente;
        } else {
            inscripcion = inscripcionComisionRepository.save(
                    new InscripcionComision(inscripcionCarreraId, comisionMateria.id(), periodo, hoy));
        }
        return toResponse(inscripcion, comisionMateria);
    }

    /** Puede estar inscripto a más de un plan (por ejemplo, de dos carreras): vale el de la comisión. */
    private Long inscripcionCarreraActiva(Long estudianteId, Long planEstudioId) {
        return personasApi.inscripcionesCarrera(estudianteId).stream()
                .filter(inscripcion -> inscripcion.activa() && inscripcion.planEstudioId().equals(planEstudioId))
                .map(InscripcionCarreraResumen::id)
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "No tenés una inscripción activa al plan de estudio de esta comisión"));
    }

    private PeriodoInscripcion periodoAbierto(ComisionMateriaResumen comisionMateria) {
        Long carreraId = carrerasApi.buscarPlan(comisionMateria.planEstudioId())
                .map(PlanEstudioResumen::carreraId)
                .orElseThrow();
        return periodoInscripcionRepository.abiertos(carreraId, TipoPeriodo.CURSADA, LocalDateTime.now()).stream()
                .filter(periodo -> periodo.admite(comisionMateria.anioLectivo(), comisionMateria.periodoLectivo()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "No hay un período de inscripción abierto para esta materia"));
    }

    // En un mismo año lectivo, una materia se cursa en una sola comisión.
    private void validarNoInscriptoEnOtraComision(Long inscripcionCarreraId, ComisionMateriaResumen comisionMateria) {
        List<Long> mismaMateria = comisionesApi.idsDeComisionesMateria(
                comisionMateria.materiaId(), comisionMateria.anioLectivo());
        if (inscripcionComisionRepository.existsByInscripcionCarreraIdAndComisionMateriaIdInAndEstado(
                inscripcionCarreraId, mismaMateria, EstadoInscripcion.INSCRIPTO)) {
            throw new ReglaNegocioException("Ya estás inscripto a esta materia en otra comisión de "
                    + comisionMateria.anioLectivo());
        }
    }

    // Las reglas son de SituacionAcademica; acá sólo se arma el mensaje.
    private void validarSituacionAcademica(Long inscripcionCarreraId, ComisionMateriaResumen comisionMateria) {
        SituacionAcademica situacion = situacionAcademicaService.de(inscripcionCarreraId,
                comisionMateria.planEstudioId());
        Long materiaId = comisionMateria.materiaId();
        if (situacion.puedeCursar(materiaId)) {
            return;
        }
        EstadoMateria estado = situacion.estado(materiaId);
        if (estado == EstadoMateria.APROBADA || estado == EstadoMateria.REGULAR) {
            throw new ReglaNegocioException("Ya tenés esta materia " + estado.name().toLowerCase());
        }
        Map<Long, String> nombres = carrerasApi.materiasDelPlan(comisionMateria.planEstudioId()).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
        throw new ReglaNegocioException("Te faltan correlativas para cursar esta materia: "
                + situacion.faltanParaCursar(materiaId).stream().map(nombres::get).collect(Collectors.joining(", ")));
    }

    // Seguro porque la materia de la comisión está bloqueada: nadie más cuenta hasta el commit.
    private void validarCupo(ComisionMateriaResumen comisionMateria) {
        if (comisionMateria.cupoMaximo() == null) {
            return;
        }
        long inscriptos = inscripcionComisionRepository.countByComisionMateriaIdAndEstado(
                comisionMateria.id(), EstadoInscripcion.INSCRIPTO);
        if (inscriptos >= comisionMateria.cupoMaximo()) {
            throw new ReglaNegocioException("No hay cupo en la comisión " + comisionMateria.nombreComision());
        }
    }

    private InscripcionComisionResponse toResponse(InscripcionComision inscripcion,
                                                   ComisionMateriaResumen comisionMateria) {
        String nombreMateria = carrerasApi.buscarMateriaDelPlan(comisionMateria.planEstudioId(),
                        comisionMateria.materiaId())
                .map(MateriaDelPlan::nombre)
                .orElse(null);
        return inscripcionComisionMapper.toResponse(inscripcion, comisionMateria, nombreMateria);
    }
}
