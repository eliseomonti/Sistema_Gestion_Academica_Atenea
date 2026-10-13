package com.utn.atenea.carreras.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "plan_estudio")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrera_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Carrera carrera;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "anio_inicio")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer anioInicio;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean vigente = true;

    @Column(name = "fecha_baja")
    @Setter(AccessLevel.NONE)
    private LocalDate fechaBaja;

    public PlanEstudio(Carrera carrera, String nombre, Integer anioInicio) {
        this.carrera = carrera;
        this.nombre = nombre;
        this.anioInicio = anioInicio;
    }

    public void darDeBaja(LocalDate fecha) {
        this.vigente = false;
        this.fechaBaja = fecha;
    }

    public void reactivar() {
        this.vigente = true;
        this.fechaBaja = null;
    }
}
