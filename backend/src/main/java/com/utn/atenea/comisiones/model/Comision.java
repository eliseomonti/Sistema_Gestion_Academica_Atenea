package com.utn.atenea.comisiones.model;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.Turno;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Grupo de estudiantes de un plan que cursa junto las materias de un año de la carrera. */
@Entity
@Table(name = "comision")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    // plan_estudio es del módulo carreras: se referencia por id.
    @Column(name = "plan_estudio_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long planEstudioId;

    // Año de la carrera, como PlanMateria.anioCursado.
    @Column(name = "anio_cursado", nullable = false)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int anioCursado;

    @Column(name = "nro_comision", nullable = false)
    private int nroComision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Turno turno;

    @Column(name = "anio_lectivo", nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int anioLectivo;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean activa = true;

    @Column(name = "fecha_baja")
    @Setter(AccessLevel.NONE)
    private LocalDate fechaBaja;

    @OneToMany(mappedBy = "comision", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<ComisionMateria> materias = new ArrayList<>();

    public Comision(Long planEstudioId, int anioCursado, int nroComision, Turno turno, int anioLectivo) {
        this.planEstudioId = planEstudioId;
        this.anioCursado = anioCursado;
        this.nroComision = nroComision;
        this.turno = turno;
        this.anioLectivo = anioLectivo;
    }

    /**
     * Como figura en la planilla: el número y los dos últimos dígitos de la cohorte, el año en que
     * entró el grupo. Por ejemplo, la comisión 1 de 2° año en 2026 es la "1/25".
     */
    public String getNombre() {
        int cohorte = anioLectivo - anioCursado + 1;
        return nroComision + "/" + String.format("%02d", cohorte % 100);
    }

    public List<ComisionMateria> getMaterias() {
        return Collections.unmodifiableList(materias);
    }

    /** periodoLectivo es el de la materia en el plan (PlanMateria), no lo elige el admin. */
    public ComisionMateria agregarMateria(Long materiaId, PeriodoLectivo periodoLectivo, Integer cupoMaximo) {
        ComisionMateria materia = new ComisionMateria(this, materiaId, periodoLectivo, cupoMaximo);
        materias.add(materia);
        return materia;
    }

    public void quitarMateria(ComisionMateria materia) {
        materias.remove(materia);
    }

    public void darDeBaja(LocalDate fecha) {
        this.activa = false;
        this.fechaBaja = fecha;
    }

    public void reactivar() {
        this.activa = true;
        this.fechaBaja = null;
    }
}
