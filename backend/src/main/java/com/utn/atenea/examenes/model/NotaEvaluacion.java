package com.utn.atenea.examenes.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Nota de un estudiante en una evaluación de la cursada. Igual que NotaFinal: se inserta y no se
 * modifica nunca, corregirla es rectificarla y la vigente es la última de la cadena.
 */
@Entity
@Immutable
@Table(name = "nota_evaluacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotaEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Redundante: es la de la evaluación y la de la inscripción. Las dos FK compuestas de la tabla la
    // comparten, así que la base rechaza una inscripción a otra materia de comisión. Se copia de la evaluación.
    @Column(name = "comision_materia_id", nullable = false)
    private Long comisionMateriaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_cursada_id", nullable = false)
    private EvaluacionCursada evaluacion;

    // estudiante_inscripcion_comision es del módulo inscripciones: se referencia por id.
    @Column(name = "inscripcion_comision_id", nullable = false)
    private Long inscripcionComisionId;

    // Quien cargó la nota. persona es del módulo personas: se referencia por id.
    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    // Sale de la nota (CHECK chk_condicion_nota_evaluacion): AUSENTE si y sólo si no hay nota.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondicionExamen condicion;

    // Escala entera de 0 a 10.
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer nota;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    // La que ésta corrige; NULL en la original. Ver NotaFinal.notaRectificada.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_rectificada_id")
    private NotaEvaluacion notaRectificada;

    /** nota en null: el estudiante estuvo ausente. */
    public NotaEvaluacion(EvaluacionCursada evaluacion, Long inscripcionComisionId, Long docenteId, Integer nota,
                          LocalDate fechaRegistro) {
        this(evaluacion, inscripcionComisionId, docenteId, nota, fechaRegistro, null);
    }

    private NotaEvaluacion(EvaluacionCursada evaluacion, Long inscripcionComisionId, Long docenteId, Integer nota,
                           LocalDate fechaRegistro, NotaEvaluacion notaRectificada) {
        this.comisionMateriaId = evaluacion.getComisionMateriaId();
        this.evaluacion = evaluacion;
        this.inscripcionComisionId = inscripcionComisionId;
        this.docenteId = docenteId;
        this.condicion = nota == null ? CondicionExamen.AUSENTE : CondicionExamen.PRESENTE;
        this.nota = nota;
        this.fechaRegistro = fechaRegistro;
        this.notaRectificada = notaRectificada;
    }

    /** La corrección de esta nota. Sólo se rectifica la vigente: el UNIQUE (nota_rectificada_id) rechaza otra. */
    public NotaEvaluacion rectificar(Long docenteId, Integer nota, LocalDate fechaRegistro) {
        return new NotaEvaluacion(evaluacion, inscripcionComisionId, docenteId, nota, fechaRegistro, this);
    }
}
