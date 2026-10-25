package com.utn.atenea.comisiones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.comisiones.dto.AltaComisionRequest;
import com.utn.atenea.comisiones.dto.ComisionDetalleResponse;
import com.utn.atenea.comisiones.dto.ComisionFiltroRequest;
import com.utn.atenea.comisiones.dto.ComisionMateriaResponse;
import com.utn.atenea.comisiones.dto.ComisionResponse;
import com.utn.atenea.comisiones.dto.ModificacionComisionRequest;
import com.utn.atenea.comisiones.mapper.ComisionMapper;
import com.utn.atenea.comisiones.mapper.ComisionMateriaMapper;
import com.utn.atenea.comisiones.model.Comision;
import com.utn.atenea.comisiones.model.ComisionDocente;
import com.utn.atenea.comisiones.repository.ComisionRepository;
import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** El grupo: listado, detalle, alta, modificación, baja y borrado. Sus materias, en ComisionMateriaService. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComisionService {

    private final ComisionRepository comisionRepository;
    private final CarrerasApi carrerasApi;
    private final PersonasApi personasApi;
    private final ComisionMapper comisionMapper;
    private final ComisionMateriaMapper comisionMateriaMapper;

    public List<ComisionResponse> listar(ComisionFiltroRequest filtro) {
        List<Long> planes = carrerasApi.idsDePlanes(filtro.carreraId());
        if (filtro.planEstudioId() != null) {
            if (!planes.contains(filtro.planEstudioId())) {
                throw new RecursoNoEncontradoException("No existe el plan de estudio con id "
                        + filtro.planEstudioId() + " en la carrera con id " + filtro.carreraId());
            }
            planes = List.of(filtro.planEstudioId());
        }
        if (planes.isEmpty()) {
            return List.of();
        }

        int anioLectivo = filtro.anioLectivo() != null ? filtro.anioLectivo() : LocalDate.now().getYear();
        boolean activa = filtro.activa() == null || filtro.activa();
        return comisionMapper.toResponse(comisionRepository.listar(planes, anioLectivo, activa,
                filtro.anioCursado(), filtro.turno(), filtro.periodoLectivo()));
    }

    /** También el de una comisión dada de baja: su historia se sigue consultando. */
    public ComisionDetalleResponse detalle(Long comisionId) {
        Comision comision = buscar(comisionId);

        // Los nombres de las materias y de los docentes se piden una sola vez, para toda la comisión.
        Map<Long, String> nombresMaterias = carrerasApi.materiasDelPlan(comision.getPlanEstudioId()).stream()
                .collect(Collectors.toMap(MateriaDelPlan::materiaId, MateriaDelPlan::nombre));
        Set<Long> docenteIds = comision.getMaterias().stream()
                .flatMap(materia -> materia.getDocentes().stream())
                .map(ComisionDocente::getDocenteId)
                .collect(Collectors.toSet());
        Map<Long, DocenteResumen> docentes = personasApi.buscarDocentes(docenteIds).stream()
                .collect(Collectors.toMap(DocenteResumen::personaId, Function.identity()));

        List<ComisionMateriaResponse> materias = comision.getMaterias().stream()
                .map(materia -> comisionMateriaMapper.toResponse(materia, nombresMaterias, docentes))
                .sorted(Comparator.comparing(ComisionMateriaResponse::periodoLectivo)
                        .thenComparing(ComisionMateriaResponse::nombreMateria))
                .toList();
        return comisionMapper.toDetalleResponse(comision, materias);
    }

    /** Un plan discontinuado también admite comisiones: sus estudiantes siguen cursando. */
    @Transactional
    public ComisionResponse alta(AltaComisionRequest request) {
        if (carrerasApi.buscarPlan(request.planEstudioId()).isEmpty()) {
            throw new RecursoNoEncontradoException("No existe el plan de estudio con id " + request.planEstudioId());
        }
        validarAnioCursado(request.planEstudioId(), request.anioCursado());
        validarNumeroLibre(request.planEstudioId(), request.anioCursado(), request.nroComision(),
                request.anioLectivo(), null);

        Comision comision = comisionRepository.save(new Comision(request.planEstudioId(), request.anioCursado(),
                request.nroComision(), request.turno(), request.anioLectivo()));
        return comisionMapper.toResponse(comision);
    }

    @Transactional
    public ComisionResponse modificar(Long comisionId, ModificacionComisionRequest request) {
        Comision comision = buscarActiva(comisionId);
        validarNumeroLibre(comision.getPlanEstudioId(), comision.getAnioCursado(), request.nroComision(),
                comision.getAnioLectivo(), comisionId);

        comision.setNroComision(request.nroComision());
        comision.setTurno(request.turno());
        return comisionMapper.toResponse(comision);
    }

    /**
     * Baja lógica de una comisión con historia: deja de listarse por defecto, sus horarios ya no
     * ocupan aulas ni docentes y no se puede modificar. Reactivar queda fuera del MVP.
     */
    @Transactional
    public ComisionResponse darDeBaja(Long comisionId) {
        Comision comision = buscar(comisionId);
        if (!comision.isActiva()) {
            throw new ReglaNegocioException("La comisión " + comisionId + " ya está dada de baja");
        }
        comision.darDeBaja(LocalDate.now());
        return comisionMapper.toResponse(comision);
    }

    /**
     * Borra la comisión con sus materias, docentes y horarios: es para corregir una carga errónea.
     * Si alguna materia ya tiene inscripciones, evaluaciones o encuestas, las FK RESTRICT de la base
     * lo impiden. comisiones no puede preguntárselo a inscripciones ni a examenes (están más arriba),
     * así que el control es la base.
     */
    @Transactional
    public void eliminar(Long comisionId) {
        comisionRepository.delete(buscar(comisionId));
        try {
            comisionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("La comisión " + comisionId
                    + " tiene inscripciones, evaluaciones o encuestas: no se puede borrar, sólo dar de baja");
        }
    }

    /** La comisión a la que se le cambia algo: tiene que existir y no estar dada de baja. */
    Comision buscarActiva(Long comisionId) {
        Comision comision = buscar(comisionId);
        if (!comision.isActiva()) {
            throw new ReglaNegocioException("La comisión " + comisionId + " está dada de baja");
        }
        return comision;
    }

    private Comision buscar(Long comisionId) {
        return comisionRepository.findById(comisionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la comisión con id " + comisionId));
    }

    private void validarAnioCursado(Long planEstudioId, int anioCursado) {
        int anios = carrerasApi.cantidadDeAnios(planEstudioId);
        if (anios == 0) {
            throw new ReglaNegocioException("El plan de estudio " + planEstudioId + " no tiene materias cargadas");
        }
        if (anioCursado > anios) {
            throw new ReglaNegocioException("El plan de estudio " + planEstudioId + " tiene " + anios
                    + " años: no tiene materias de " + anioCursado + "° año");
        }
    }

    private void validarNumeroLibre(Long planEstudioId, int anioCursado, int nroComision, int anioLectivo,
                                    Long excluirComisionId) {
        if (comisionRepository.numeroEnUso(planEstudioId, anioCursado, nroComision, anioLectivo, excluirComisionId)) {
            throw new RecursoDuplicadoException("Ya existe la comisión " + nroComision + " de " + anioCursado
                    + "° año en el plan de estudio " + planEstudioId + " para " + anioLectivo);
        }
    }
}
