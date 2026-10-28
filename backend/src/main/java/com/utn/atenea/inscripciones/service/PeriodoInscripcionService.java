package com.utn.atenea.inscripciones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.inscripciones.dto.AltaPeriodoInscripcionRequest;
import com.utn.atenea.inscripciones.dto.ModificacionPeriodoInscripcionRequest;
import com.utn.atenea.inscripciones.dto.PeriodoInscripcionFiltroRequest;
import com.utn.atenea.inscripciones.dto.PeriodoInscripcionResponse;
import com.utn.atenea.inscripciones.mapper.PeriodoInscripcionMapper;
import com.utn.atenea.inscripciones.model.PeriodoInscripcion;
import com.utn.atenea.inscripciones.model.TipoPeriodo;
import com.utn.atenea.inscripciones.repository.PeriodoInscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** ABM de las ventanas de inscripción a cursadas y a finales. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeriodoInscripcionService {

    private final PeriodoInscripcionRepository periodoInscripcionRepository;
    private final CarrerasApi carrerasApi;
    private final PeriodoInscripcionMapper periodoInscripcionMapper;

    public List<PeriodoInscripcionResponse> listar(PeriodoInscripcionFiltroRequest filtro) {
        int anioLectivo = filtro.anioLectivo() != null ? filtro.anioLectivo() : LocalDate.now().getYear();
        return periodoInscripcionMapper.toResponse(
                periodoInscripcionRepository.listar(filtro.carreraId(), anioLectivo, filtro.tipo()));
    }

    public PeriodoInscripcionResponse detalle(Long periodoId) {
        return periodoInscripcionMapper.toResponse(buscar(periodoId));
    }

    @Transactional
    public PeriodoInscripcionResponse alta(AltaPeriodoInscripcionRequest request) {
        if (!carrerasApi.existeCarrera(request.carreraId())) {
            throw new RecursoNoEncontradoException("No existe la carrera con id " + request.carreraId());
        }
        validarSinSuperposicion(request.carreraId(), request.tipo(), request.fechaInicio(), request.fechaCierre(), null);

        PeriodoInscripcion periodo = request.tipo() == TipoPeriodo.CURSADA
                ? PeriodoInscripcion.deCursada(request.carreraId(), request.anioLectivo(), request.periodoLectivo(),
                        request.fechaInicio(), request.fechaCierre())
                : PeriodoInscripcion.deFinal(request.carreraId(), request.anioLectivo(),
                        request.fechaInicio(), request.fechaCierre());
        return periodoInscripcionMapper.toResponse(periodoInscripcionRepository.save(periodo));
    }

    /** Sirve también para extender un período ya abierto o reabrir uno cerrado. */
    @Transactional
    public PeriodoInscripcionResponse modificar(Long periodoId, ModificacionPeriodoInscripcionRequest request) {
        PeriodoInscripcion periodo = buscar(periodoId);
        validarSinSuperposicion(periodo.getCarreraId(), periodo.getTipo(), request.fechaInicio(),
                request.fechaCierre(), periodoId);

        periodo.cambiarFechas(request.fechaInicio(), request.fechaCierre());
        return periodoInscripcionMapper.toResponse(periodo);
    }

    @Transactional
    public PeriodoInscripcionResponse habilitar(Long periodoId) {
        PeriodoInscripcion periodo = buscar(periodoId);
        if (periodo.isHabilitada()) {
            throw new ReglaNegocioException("El período de inscripción " + periodoId + " ya está habilitado");
        }
        periodo.habilitar();
        return periodoInscripcionMapper.toResponse(periodo);
    }

    /** Suspende las inscripciones sin tocar las fechas; las ya hechas se conservan. */
    @Transactional
    public PeriodoInscripcionResponse deshabilitar(Long periodoId) {
        PeriodoInscripcion periodo = buscar(periodoId);
        if (!periodo.isHabilitada()) {
            throw new ReglaNegocioException("El período de inscripción " + periodoId + " ya está deshabilitado");
        }
        periodo.deshabilitar();
        return periodoInscripcionMapper.toResponse(periodo);
    }

    /**
     * Borra un período mal cargado. Si ya tiene inscripciones a cursada o mesas de examen, las FK
     * RESTRICT de la base lo impiden: las mesas son de examenes, así que el control es la base.
     */
    @Transactional
    public void eliminar(Long periodoId) {
        periodoInscripcionRepository.delete(buscar(periodoId));
        try {
            periodoInscripcionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("El período de inscripción " + periodoId
                    + " tiene inscripciones o mesas de examen: no se puede borrar, sólo deshabilitar");
        }
    }

    private PeriodoInscripcion buscar(Long periodoId) {
        return periodoInscripcionRepository.findById(periodoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el período de inscripción con id " + periodoId));
    }

    // Dos ventanas del mismo tipo abiertas a la vez dejan ambiguo en cuál se anota el estudiante.
    private void validarSinSuperposicion(Long carreraId, TipoPeriodo tipo, LocalDateTime fechaInicio,
                                         LocalDateTime fechaCierre, Long excluirPeriodoId) {
        if (periodoInscripcionRepository.seSuperpone(carreraId, tipo, fechaInicio, fechaCierre, excluirPeriodoId)) {
            throw new ReglaNegocioException("La carrera " + carreraId + " ya tiene un período de inscripción a "
                    + tipo + " que se superpone con esas fechas");
        }
    }
}
