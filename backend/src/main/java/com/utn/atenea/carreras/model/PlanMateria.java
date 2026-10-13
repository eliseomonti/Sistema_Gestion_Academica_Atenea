package com.utn.atenea.carreras.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plan_materia")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_estudio_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private PlanEstudio planEstudio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Materia materia;

    @Column(length = 10)
    private String codigo;

    @Column(name = "anio_cursado", nullable = false)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int anioCursado;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_lectivo", nullable = false)
    private PeriodoLectivo periodoLectivo;

    public PlanMateria(PlanEstudio planEstudio, Materia materia, String codigo,
                       int anioCursado, PeriodoLectivo periodoLectivo) {
        this.planEstudio = planEstudio;
        this.materia = materia;
        this.codigo = codigo;
        this.anioCursado = anioCursado;
        this.periodoLectivo = periodoLectivo;
    }
}
