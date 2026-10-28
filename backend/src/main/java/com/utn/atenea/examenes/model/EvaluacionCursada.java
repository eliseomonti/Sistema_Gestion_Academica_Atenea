package com.utn.atenea.examenes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Instancia de evaluación de una materia de una comisión: un parcial, un TP, etc. Un recuperatorio es
 * otra evaluación que apunta a la que recupera.
 */
@Entity
@Table(name = "evaluacion_cursada")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluacionCursada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    // comision_materia es del módulo comisiones: se referencia por id.
    @Column(name = "comision_materia_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long comisionMateriaId;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fecha;

    // Fijo: el recuperatorio lo copia al crearse.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private TipoEvaluacion tipo;

    // NULL en una instancia original. La FK compuesta fk_eval_recuperada exige que sea de la misma
    // materia de la comisión: por eso un recuperatorio sólo se crea con recuperatorio().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_recuperada_id", updatable = false)
    @Setter(AccessLevel.NONE)
    private EvaluacionCursada evaluacionRecuperada;

    public EvaluacionCursada(Long comisionMateriaId, String nombre, LocalDate fecha, TipoEvaluacion tipo) {
        this(comisionMateriaId, nombre, fecha, tipo, null);
    }

    private EvaluacionCursada(Long comisionMateriaId, String nombre, LocalDate fecha, TipoEvaluacion tipo,
                              EvaluacionCursada evaluacionRecuperada) {
        this.comisionMateriaId = comisionMateriaId;
        this.nombre = nombre;
        this.fecha = fecha;
        this.tipo = tipo;
        this.evaluacionRecuperada = evaluacionRecuperada;
    }

    /** El recuperatorio de esta evaluación: misma materia de la comisión y mismo tipo. */
    public EvaluacionCursada recuperatorio(String nombre, LocalDate fecha) {
        return new EvaluacionCursada(comisionMateriaId, nombre, fecha, tipo, this);
    }

    public boolean esRecuperatorio() {
        return evaluacionRecuperada != null;
    }
}
