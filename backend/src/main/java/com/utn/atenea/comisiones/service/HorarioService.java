package com.utn.atenea.comisiones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaDelPlan;
import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.dto.HorarioRequest;
import com.utn.atenea.comisiones.model.Aula;
import com.utn.atenea.comisiones.model.Comision;
import com.utn.atenea.comisiones.model.ComisionHorario;
import com.utn.atenea.comisiones.model.ComisionMateria;
import com.utn.atenea.comisiones.model.DiaSemana;
import com.utn.atenea.comisiones.repository.AulaRepository;
import com.utn.atenea.comisiones.repository.ComisionHorarioRepository;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.personas.DocenteResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Los choques de horario de una materia de la comisión: que sus franjas no se pisen entre sí ni con
 * las otras materias del grupo, que el aula esté libre y que sus docentes no estén dando clase en
 * otra materia a esa hora. Sólo cuentan las comisiones activas del mismo año lectivo y las materias
 * cuyos períodos se pisan.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HorarioService {

    private final ComisionHorarioRepository comisionHorarioRepository;
    private final AulaRepository aulaRepository;
    private final CarrerasApi carrerasApi;

    /**
     * Valida los horarios que va a tener la materia y devuelve sus aulas por id, para asignarlas.
     * actual es la materia que se modifica (null en el alta): sus horarios de hoy se reemplazan,
     * así que no cuentan como choque.
     */
    public Map<Long, Aula> validar(Comision comision, ComisionMateria actual, PeriodoLectivo periodo,
                                   List<HorarioRequest> horarios, Map<Long, DocenteResumen> docentes) {
        validarQueNoSePisanEntreSi(horarios);
        Map<Long, Aula> aulas = buscarAulas(horarios);
        validarContraElGrupo(comision, actual, periodo, horarios);

        Long excluir = actual != null ? actual.getId() : null;
        for (HorarioRequest horario : horarios) {
            validarAulaLibre(horario, aulas, comision.getAnioLectivo(), periodo, excluir);
            validarDocentesLibres(horario, docentes, comision.getAnioLectivo(), periodo, excluir);
        }
        return aulas;
    }

    private void validarQueNoSePisanEntreSi(List<HorarioRequest> horarios) {
        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {
                HorarioRequest uno = horarios.get(i);
                HorarioRequest otro = horarios.get(j);
                if (uno.diaSemana() == otro.diaSemana()
                        && uno.horaInicio().isBefore(otro.horaFin()) && uno.horaFin().isAfter(otro.horaInicio())) {
                    throw new ReglaNegocioException("Los horarios del " + texto(uno) + " y de "
                            + otro.horaInicio() + " a " + otro.horaFin() + " se superponen");
                }
            }
        }
    }

    private Map<Long, Aula> buscarAulas(List<HorarioRequest> horarios) {
        Set<Long> ids = horarios.stream()
                .map(HorarioRequest::aulaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Aula> aulas = aulaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Aula::getId, Function.identity()));
        for (Long id : ids) {
            Aula aula = aulas.get(id);
            if (aula == null) {
                throw new RecursoNoEncontradoException("No existe el aula con id " + id);
            }
            if (!aula.isActiva()) {
                throw new ReglaNegocioException("El aula " + aula.getNombre() + " está dada de baja");
            }
        }
        return aulas;
    }

    // El grupo cursa todas sus materias: dos que se dictan a la vez en la misma parte del año chocan,
    // aunque sea en aulas distintas o virtuales.
    private void validarContraElGrupo(Comision comision, ComisionMateria actual, PeriodoLectivo periodo,
                                      List<HorarioRequest> horarios) {
        for (ComisionMateria otra : comision.getMaterias()) {
            boolean esLaQueSeModifica = actual != null && otra.getId().equals(actual.getId());
            if (esLaQueSeModifica || !otra.getPeriodoLectivo().seSuperponeCon(periodo)) {
                continue;
            }
            for (ComisionHorario existente : otra.getHorarios()) {
                for (HorarioRequest horario : horarios) {
                    if (existente.seSuperponeCon(horario.diaSemana(), horario.horaInicio(), horario.horaFin())) {
                        throw new ReglaNegocioException("El horario del " + texto(horario) + " se superpone con "
                                + nombreMateria(comision, otra) + ", que la comisión cursa el "
                                + texto(existente.getDiaSemana(), existente.getHoraInicio(), existente.getHoraFin()));
                    }
                }
            }
        }
    }

    private void validarAulaLibre(HorarioRequest horario, Map<Long, Aula> aulas, int anioLectivo,
                                  PeriodoLectivo periodo, Long excluir) {
        if (horario.aulaId() == null) {
            return;
        }
        if (comisionHorarioRepository.aulaOcupada(horario.aulaId(), horario.diaSemana(), horario.horaInicio(),
                horario.horaFin(), anioLectivo, periodo.superpuestos(), excluir)) {
            throw new ReglaNegocioException("El aula " + aulas.get(horario.aulaId()).getNombre()
                    + " ya está ocupada el " + texto(horario));
        }
    }

    private void validarDocentesLibres(HorarioRequest horario, Map<Long, DocenteResumen> docentes,
                                       int anioLectivo, PeriodoLectivo periodo, Long excluir) {
        if (docentes.isEmpty()) {
            return;
        }
        List<Long> ocupados = comisionHorarioRepository.docentesOcupados(docentes.keySet(), horario.diaSemana(),
                horario.horaInicio(), horario.horaFin(), anioLectivo, periodo.superpuestos(), excluir);
        if (!ocupados.isEmpty()) {
            DocenteResumen docente = docentes.get(ocupados.getFirst());
            throw new ReglaNegocioException("El docente " + docente.apellido() + ", " + docente.nombre()
                    + " ya da clase el " + texto(horario) + " en otra materia");
        }
    }

    private String nombreMateria(Comision comision, ComisionMateria materia) {
        return carrerasApi.buscarMateriaDelPlan(comision.getPlanEstudioId(), materia.getMateriaId())
                .map(MateriaDelPlan::nombre)
                .orElse("otra materia");
    }

    private static String texto(HorarioRequest horario) {
        return texto(horario.diaSemana(), horario.horaInicio(), horario.horaFin());
    }

    // Por ejemplo "lunes de 08:30 a 10:30".
    private static String texto(DiaSemana dia, LocalTime inicio, LocalTime fin) {
        return dia.name().toLowerCase() + " de " + inicio + " a " + fin;
    }
}
