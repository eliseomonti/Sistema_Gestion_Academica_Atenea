package com.utn.atenea.comisiones.model;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.DiaSemana;
import com.utn.atenea.comisiones.RolDocente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.*;

/** Una materia que se dicta en una comisión: es a lo que se inscribe el estudiante. */
@Entity
@Table(name = "comision_materia")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComisionMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comision_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Comision comision;

    // materia es del módulo carreras: se referencia por id.
    @Column(name = "materia_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long materiaId;

    // Copia del período de la materia en el plan: lo pone Comision.agregarMateria.
    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_lectivo", nullable = false)
    @Setter(AccessLevel.NONE)
    private PeriodoLectivo periodoLectivo;

    // NULL = sin límite de cupo.
    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @ElementCollection
    @CollectionTable(name = "comision_docente", joinColumns = @JoinColumn(name = "comision_materia_id"))
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Set<ComisionDocente> docentes = new HashSet<>();

    @OneToMany(mappedBy = "comisionMateria", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<ComisionHorario> horarios = new ArrayList<>();

    // Se crea desde Comision.agregarMateria, que mantiene los dos lados de la relación.
    ComisionMateria(Comision comision, Long materiaId, PeriodoLectivo periodoLectivo, Integer cupoMaximo) {
        this.comision = comision;
        this.materiaId = materiaId;
        this.periodoLectivo = periodoLectivo;
        this.cupoMaximo = cupoMaximo;
    }

    public Set<ComisionDocente> getDocentes() {
        return Collections.unmodifiableSet(docentes);
    }

    public List<ComisionHorario> getHorarios() {
        return Collections.unmodifiableList(horarios);
    }

    /** Asigna al docente con ese rol; si ya estaba asignado, le cambia el rol. */
    public void asignarDocente(Long docenteId, RolDocente rol) {
        docentes.removeIf(d -> d.getDocenteId().equals(docenteId));
        docentes.add(new ComisionDocente(docenteId, rol));
    }

    public ComisionHorario agregarHorario(DiaSemana diaSemana, LocalTime horaInicio,
                                          LocalTime horaFin, Aula aula) {
        ComisionHorario horario = new ComisionHorario(this, diaSemana, horaInicio, horaFin, aula);
        horarios.add(horario);
        return horario;
    }

    /** La modificación reemplaza docentes y horarios completos: primero se quitan todos. */
    public void quitarDocentesYHorarios() {
        docentes.clear();
        horarios.clear();
    }
}
