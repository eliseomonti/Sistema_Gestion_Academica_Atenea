package com.utn.atenea.inscripciones.model;

import com.utn.atenea.carreras.PeriodoLectivo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Ventana de inscripción de una carrera: una por cuatrimestre para las cursadas y una por turno
 * para los finales.
 */
@Entity
@Table(name = "periodo_inscripcion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PeriodoInscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // carrera es del módulo carreras: se referencia por id.
    @Column(name = "carrera_id", nullable = false, updatable = false)
    private Long carreraId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoPeriodo tipo;

    @Column(name = "anio_lectivo", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int anioLectivo;

    // Sólo en los de CURSADA (CHECK chk_periodo_tipo_lectivo): por eso se crea con deCursada/deFinal.
    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_lectivo", updatable = false)
    private PeriodoLectivo periodoLectivo;

    // fechaInicio y fechaCierre cambian juntas (CHECK chk_periodo_fechas): sólo con cambiarFechas.
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Column(nullable = false)
    private boolean habilitada = true;

    private PeriodoInscripcion(Long carreraId, TipoPeriodo tipo, int anioLectivo, PeriodoLectivo periodoLectivo,
                               LocalDateTime fechaInicio, LocalDateTime fechaCierre) {
        this.carreraId = carreraId;
        this.tipo = tipo;
        this.anioLectivo = anioLectivo;
        this.periodoLectivo = periodoLectivo;
        this.fechaInicio = fechaInicio;
        this.fechaCierre = fechaCierre;
    }

    public static PeriodoInscripcion deCursada(Long carreraId, int anioLectivo, PeriodoLectivo periodoLectivo,
                                               LocalDateTime fechaInicio, LocalDateTime fechaCierre) {
        return new PeriodoInscripcion(carreraId, TipoPeriodo.CURSADA, anioLectivo, periodoLectivo,
                fechaInicio, fechaCierre);
    }

    public static PeriodoInscripcion deFinal(Long carreraId, int anioLectivo,
                                             LocalDateTime fechaInicio, LocalDateTime fechaCierre) {
        return new PeriodoInscripcion(carreraId, TipoPeriodo.FINAL, anioLectivo, null, fechaInicio, fechaCierre);
    }

    /** Habilitado y con ahora dentro de [fechaInicio, fechaCierre). */
    public boolean estaAbierto(LocalDateTime ahora) {
        return habilitada && !ahora.isBefore(fechaInicio) && ahora.isBefore(fechaCierre);
    }

    /**
     * Una ventana de cursada admite las materias de su año lectivo que se empiezan a cursar en ella:
     * la del primer cuatrimestre, las del primero y las anuales; la del segundo, sólo las del segundo;
     * una anual, todas las del año.
     */
    public boolean admite(int anioLectivoMateria, PeriodoLectivo periodoMateria) {
        if (anioLectivo != anioLectivoMateria) {
            return false;
        }
        return switch (periodoLectivo) {
            case ANUAL -> true;
            case PRIMER_CUATRIMESTRE -> periodoMateria != PeriodoLectivo.SEGUNDO_CUATRIMESTRE;
            case SEGUNDO_CUATRIMESTRE -> periodoMateria == PeriodoLectivo.SEGUNDO_CUATRIMESTRE;
        };
    }

    public void cambiarFechas(LocalDateTime fechaInicio, LocalDateTime fechaCierre) {
        this.fechaInicio = fechaInicio;
        this.fechaCierre = fechaCierre;
    }

    public void habilitar() {
        this.habilitada = true;
    }

    public void deshabilitar() {
        this.habilitada = false;
    }
}
