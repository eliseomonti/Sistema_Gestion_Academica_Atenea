package com.utn.atenea.carreras.model;

import com.utn.atenea.carreras.EstadoCorrelativa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "correlatividad_materias")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Correlatividad {

    @EmbeddedId
    @Setter(AccessLevel.NONE)
    private CorrelatividadId id;

    @MapsId("planMateriaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_materia_id")
    @Setter(AccessLevel.NONE)
    private PlanMateria planMateria;

    @MapsId("correlativaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_materia_correlativa_id")
    @Setter(AccessLevel.NONE)
    private PlanMateria correlativa;

    @Column(name = "plan_estudio_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long planEstudioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_para_cursar")
    private EstadoCorrelativa estadoParaCursar;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_para_rendir")
    private EstadoCorrelativa estadoParaRendir;

    public Correlatividad(PlanMateria planMateria, PlanMateria correlativa,
                          EstadoCorrelativa estadoParaCursar, EstadoCorrelativa estadoParaRendir) {
        this.id = new CorrelatividadId(planMateria.getId(), correlativa.getId());
        this.planMateria = planMateria;
        this.correlativa = correlativa;
        this.planEstudioId = planMateria.getPlanEstudio().getId();
        this.estadoParaCursar = estadoParaCursar;
        this.estadoParaRendir = estadoParaRendir;
    }
}
