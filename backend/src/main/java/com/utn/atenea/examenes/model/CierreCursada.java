package com.utn.atenea.examenes.model;

import com.utn.atenea.examenes.CondicionCursada;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Acta de cierre de cursada: cómo terminó un estudiante una materia de una comisión. Como las notas, se
 * inserta y no se modifica nunca: corregirla es rectificarla, y la vigente es la última de la cadena.
 */
@Entity
@Immutable
@Table(name = "cierre_cursada")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CierreCursada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // estudiante_inscripcion_comision es del módulo inscripciones: se referencia por id.
    @Column(name = "inscripcion_comision_id", nullable = false)
    private Long inscripcionComisionId;

    // El docente que firma el acta. persona es del módulo personas: se referencia por id.
    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondicionCursada condicion;

    // Escala entera de 0 a 10. Obligatoria si es PROMOCIONADO (CHECK chk_cierre_condicion_nota).
    @Column(name = "nota_cursada")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer notaCursada;

    // Fecha del acta.
    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    // Sólo en REGULAR, y posterior a fechaCierre (CHECK chk_cierre_vencimiento). Se calcula con el
    // reglamento del día del acta y se guarda: un cambio de reglamento no reescribe el pasado.
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    // La que ésta corrige; NULL en la original. Ver NotaFinal.notaRectificada.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cierre_rectificado_id")
    private CierreCursada cierreRectificado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public CierreCursada(Long inscripcionComisionId, Long docenteId, CondicionCursada condicion, Integer notaCursada,
                         LocalDate fechaCierre, LocalDate fechaVencimiento, LocalDateTime fechaRegistro) {
        this(inscripcionComisionId, docenteId, condicion, notaCursada, fechaCierre, fechaVencimiento, fechaRegistro,
                null);
    }

    private CierreCursada(Long inscripcionComisionId, Long docenteId, CondicionCursada condicion, Integer notaCursada,
                          LocalDate fechaCierre, LocalDate fechaVencimiento, LocalDateTime fechaRegistro,
                          CierreCursada cierreRectificado) {
        this.inscripcionComisionId = inscripcionComisionId;
        this.docenteId = docenteId;
        this.condicion = condicion;
        this.notaCursada = notaCursada;
        this.fechaCierre = fechaCierre;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaRegistro = fechaRegistro;
        this.cierreRectificado = cierreRectificado;
    }

    /** La corrección de esta acta. Sólo se rectifica la vigente: el UNIQUE (cierre_rectificado_id) rechaza otra. */
    public CierreCursada rectificar(Long docenteId, CondicionCursada condicion, Integer notaCursada,
                                    LocalDate fechaCierre, LocalDate fechaVencimiento, LocalDateTime fechaRegistro) {
        return new CierreCursada(inscripcionComisionId, docenteId, condicion, notaCursada, fechaCierre,
                fechaVencimiento, fechaRegistro, this);
    }
}
