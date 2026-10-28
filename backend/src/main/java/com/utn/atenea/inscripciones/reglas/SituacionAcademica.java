package com.utn.atenea.inscripciones.reglas;

import com.utn.atenea.carreras.CorrelativaDelPlan;
import com.utn.atenea.carreras.EstadoCorrelativa;
import com.utn.atenea.examenes.CondicionCursada;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La situación académica de un estudiante en un plan: el estado de cada materia y si puede cursarla o
 * rendirla según las correlativas. Java puro: recibe todo armado y no consulta nada.
 */
public class SituacionAcademica {

    /**
     * Una inscripción a comisión que no se dio de baja. condicion en null: todavía no tiene acta de
     * cierre. nota es la de cursada; fechaVencimiento, sólo si es REGULAR.
     */
    public record Cursada(Long materiaId, CondicionCursada condicion, Integer nota, LocalDate fechaVencimiento) {
    }

    /** Un final con nota vigente. nota en null: estuvo ausente. */
    public record FinalRendido(Long materiaId, Integer nota, boolean aprobado) {
    }

    private final Map<Long, EstadoMateria> estados = new HashMap<>();
    private final Map<Long, Integer> notasDeAprobacion = new HashMap<>();
    private final Map<Long, LocalDate> vencimientos = new HashMap<>();
    private final Map<Long, List<CorrelativaDelPlan>> correlativasPorMateria;

    public SituacionAcademica(Collection<CorrelativaDelPlan> correlatividades, Collection<Cursada> cursadas,
                              Collection<FinalRendido> finales, LocalDate hoy) {
        for (Cursada cursada : cursadas) {
            EstadoMateria estado = estadoDe(cursada, hoy);
            estados.merge(cursada.materiaId(), estado, SituacionAcademica::masAvanzado);
            if (estado == EstadoMateria.APROBADA) {
                notasDeAprobacion.put(cursada.materiaId(), cursada.nota());
            } else if (estado == EstadoMateria.REGULAR) {
                vencimientos.merge(cursada.materiaId(), cursada.fechaVencimiento(), SituacionAcademica::posterior);
            }
        }
        // Un final aprobado pisa a la promoción: la nota que vale es la del final.
        for (FinalRendido rendido : finales) {
            if (rendido.aprobado()) {
                estados.put(rendido.materiaId(), EstadoMateria.APROBADA);
                notasDeAprobacion.put(rendido.materiaId(), rendido.nota());
            }
        }
        correlativasPorMateria = correlatividades.stream()
                .collect(Collectors.groupingBy(CorrelativaDelPlan::materiaId));
    }

    public EstadoMateria estado(Long materiaId) {
        return estados.getOrDefault(materiaId, EstadoMateria.NO_CURSADA);
    }

    /** La del final aprobado o la de la promoción. Vacío si no está aprobada. */
    public Optional<Integer> notaDeAprobacion(Long materiaId) {
        return estado(materiaId) == EstadoMateria.APROBADA
                ? Optional.ofNullable(notasDeAprobacion.get(materiaId))
                : Optional.empty();
    }

    /** Hasta cuándo puede rendir el final. Vacío si no está REGULAR. */
    public Optional<LocalDate> vencimientoDeLaRegularidad(Long materiaId) {
        return estado(materiaId) == EstadoMateria.REGULAR
                ? Optional.ofNullable(vencimientos.get(materiaId))
                : Optional.empty();
    }

    /**
     * No la tiene aprobada ni regular, y cumple las correlativas para cursarla. CURSANDO no lo impide:
     * anotarse dos veces el mismo año lo frena la inscripción.
     */
    public boolean puedeCursar(Long materiaId) {
        EstadoMateria estado = estado(materiaId);
        return estado != EstadoMateria.APROBADA && estado != EstadoMateria.REGULAR
                && faltanParaCursar(materiaId).isEmpty();
    }

    /**
     * La tiene regular, la regularidad le sigue valiendo el día del examen (inclusive) y cumple las
     * correlativas para rendirla. No hay examen libre: sin regularidad no se rinde.
     */
    public boolean puedeRendir(Long materiaId, LocalDate fechaExamen) {
        return vencimientoDeLaRegularidad(materiaId)
                .filter(vencimiento -> !vencimiento.isBefore(fechaExamen))
                .isPresent()
                && faltanParaRendir(materiaId).isEmpty();
    }

    /** Los ids de las materias correlativas que no cumple para cursarla. */
    public List<Long> faltanParaCursar(Long materiaId) {
        return faltan(materiaId, CorrelativaDelPlan::paraCursar);
    }

    /** Los ids de las materias correlativas que no cumple para rendirla. */
    public List<Long> faltanParaRendir(Long materiaId) {
        return faltan(materiaId, CorrelativaDelPlan::paraRendir);
    }

    private List<Long> faltan(Long materiaId, Function<CorrelativaDelPlan, EstadoCorrelativa> exigido) {
        return correlativasPorMateria.getOrDefault(materiaId, List.of()).stream()
                .filter(correlativa -> exigido.apply(correlativa) != null)
                .filter(correlativa -> !cumple(estado(correlativa.correlativaId()), exigido.apply(correlativa)))
                .map(CorrelativaDelPlan::correlativaId)
                .toList();
    }

    // CURSADA se cumple con la regularidad vigente o con la materia aprobada; la regularidad vencida no alcanza.
    private static boolean cumple(EstadoMateria estado, EstadoCorrelativa exigido) {
        return switch (exigido) {
            case CURSADA -> estado == EstadoMateria.REGULAR || estado == EstadoMateria.APROBADA;
            case APROBADA -> estado == EstadoMateria.APROBADA;
        };
    }

    // La regularidad vale hasta fechaVencimiento inclusive. La promoción aprueba la materia sin final.
    private static EstadoMateria estadoDe(Cursada cursada, LocalDate hoy) {
        if (cursada.condicion() == null) {
            return EstadoMateria.CURSANDO;
        }
        return switch (cursada.condicion()) {
            case PROMOCIONADO -> EstadoMateria.APROBADA;
            case REGULAR -> hoy.isAfter(cursada.fechaVencimiento())
                    ? EstadoMateria.REGULARIDAD_VENCIDA
                    : EstadoMateria.REGULAR;
            case NO_REGULAR -> EstadoMateria.NO_REGULAR;
        };
    }

    private static LocalDate posterior(LocalDate una, LocalDate otra) {
        return una.isAfter(otra) ? una : otra;
    }

    // EstadoMateria está ordenado de más a menos avanzado.
    private static EstadoMateria masAvanzado(EstadoMateria uno, EstadoMateria otro) {
        return uno.compareTo(otro) <= 0 ? uno : otro;
    }
}
