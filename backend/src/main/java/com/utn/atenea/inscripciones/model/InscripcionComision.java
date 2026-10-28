package com.utn.atenea.inscripciones.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Inscripción de un estudiante a una materia de una comisión, dentro de un período de cursada. */
@Entity
@Table(name = "estudiante_inscripcion_comision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InscripcionComision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // inscripcion_carrera es del módulo personas: se referencia por id.
    @Column(name = "inscripcion_carrera_id", nullable = false, updatable = false)
    private Long inscripcionCarreraId;

    // comision_materia es del módulo comisiones: se referencia por id.
    @Column(name = "comision_materia_id", nullable = false, updatable = false)
    private Long comisionMateriaId;

    // La columna generada tipo_periodo no se mapea: la calcula la base y la FK compuesta
    // fk_eic_periodo rechaza un período de FINAL.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoInscripcion periodo;

    // Cambia sólo con reinscribir.
    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    // estado y fechaBaja cambian juntos (CHECK chk_eic_baja): sólo con darDeBaja y reinscribir.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInscripcion estado = EstadoInscripcion.INSCRIPTO;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    public InscripcionComision(Long inscripcionCarreraId, Long comisionMateriaId, PeriodoInscripcion periodo,
                               LocalDate fechaInscripcion) {
        this.inscripcionCarreraId = inscripcionCarreraId;
        this.comisionMateriaId = comisionMateriaId;
        this.periodo = periodo;
        this.fechaInscripcion = fechaInscripcion;
    }

    public void darDeBaja(LocalDate fecha) {
        this.estado = EstadoInscripcion.BAJA;
        this.fechaBaja = fecha;
    }

    /**
     * Vuelve a anotar a quien se había dado de baja. Se reactiva la misma fila porque el UNIQUE de la
     * tabla no deja crear otra; la baja anterior no queda registrada.
     */
    public void reinscribir(PeriodoInscripcion periodo, LocalDate fecha) {
        this.periodo = periodo;
        this.fechaInscripcion = fecha;
        this.estado = EstadoInscripcion.INSCRIPTO;
        this.fechaBaja = null;
    }
}
