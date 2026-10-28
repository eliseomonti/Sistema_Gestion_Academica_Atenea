package com.utn.atenea.examenes.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Nota de un estudiante en una mesa de final. Se inserta y no se modifica nunca: corregirla es
 * rectificarla, que agrega otra fila apuntando a ésta. La vigente es la última de la cadena.
 */
@Entity
@Immutable
@Table(name = "nota_final")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotaFinal {

    /** Con 6 o más, el final está aprobado. */
    public static final int NOTA_MINIMA_APROBACION = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // estudiante_inscripcion_examen_final es del módulo inscripciones: se referencia por id.
    @Column(name = "inscripcion_examen_final_id", nullable = false)
    private Long inscripcionExamenFinalId;

    // Quien cargó la nota. persona es del módulo personas: se referencia por id.
    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    // Sale de la nota (CHECK chk_condicion_nota_final): AUSENTE si y sólo si no hay nota.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondicionExamen condicion;

    // Escala entera de 0 a 10.
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer nota;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    // La que ésta corrige; NULL en la original. La columna generada es_original no se mapea: con los
    // UNIQUE de la tabla, impide que la cadena de rectificaciones se bifurque.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_rectificada_id")
    private NotaFinal notaRectificada;

    /** nota en null: el estudiante estuvo ausente. */
    public NotaFinal(Long inscripcionExamenFinalId, Long docenteId, Integer nota, LocalDate fechaRegistro) {
        this(inscripcionExamenFinalId, docenteId, nota, fechaRegistro, null);
    }

    private NotaFinal(Long inscripcionExamenFinalId, Long docenteId, Integer nota, LocalDate fechaRegistro,
                      NotaFinal notaRectificada) {
        this.inscripcionExamenFinalId = inscripcionExamenFinalId;
        this.docenteId = docenteId;
        this.condicion = nota == null ? CondicionExamen.AUSENTE : CondicionExamen.PRESENTE;
        this.nota = nota;
        this.fechaRegistro = fechaRegistro;
        this.notaRectificada = notaRectificada;
    }

    public boolean estaAprobada() {
        return nota != null && nota >= NOTA_MINIMA_APROBACION;
    }

    /** La corrección de esta nota. Sólo se rectifica la vigente: el UNIQUE (nota_rectificada_id) rechaza otra. */
    public NotaFinal rectificar(Long docenteId, Integer nota, LocalDate fechaRegistro) {
        return new NotaFinal(inscripcionExamenFinalId, docenteId, nota, fechaRegistro, this);
    }
}
