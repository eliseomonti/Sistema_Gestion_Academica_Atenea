package com.utn.atenea.personas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "inscripcion_carrera")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InscripcionCarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, updatable = false)
    private Persona estudiante;

    // plan_estudio es del módulo carreras: se referencia por id.
    @Column(name = "plan_estudio_id", nullable = false, updatable = false)
    private Long planEstudioId;

    @Column(name = "fecha_inscripcion", nullable = false, updatable = false)
    private LocalDate fechaInscripcion;

    // estado y fechaEstado cambian juntos (CHECK chk_inscripcion_estado_fecha): sólo con egresar/darDeBaja.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInscripcionCarrera estado = EstadoInscripcionCarrera.ACTIVA;

    @Column(name = "fecha_estado")
    private LocalDate fechaEstado;

    public InscripcionCarrera(Persona estudiante, Long planEstudioId, LocalDate fechaInscripcion) {
        this.estudiante = estudiante;
        this.planEstudioId = planEstudioId;
        this.fechaInscripcion = fechaInscripcion;
    }

    public void egresar(LocalDate fecha) {
        this.estado = EstadoInscripcionCarrera.EGRESADO;
        this.fechaEstado = fecha;
    }

    public void darDeBaja(LocalDate fecha) {
        this.estado = EstadoInscripcionCarrera.BAJA;
        this.fechaEstado = fecha;
    }
}
