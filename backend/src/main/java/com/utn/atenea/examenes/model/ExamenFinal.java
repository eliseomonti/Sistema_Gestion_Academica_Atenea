package com.utn.atenea.examenes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Mesa de examen final de una materia, dentro de un turno (un período de inscripción de tipo FINAL). */
@Entity
@Table(name = "examen_final")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamenFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // materia es del módulo carreras: se referencia por id.
    @Column(name = "materia_id", nullable = false, updatable = false)
    private Long materiaId;

    // periodo_inscripcion es del módulo inscripciones: se referencia por id. La columna generada
    // tipo_periodo no se mapea: la calcula la base y la FK compuesta fk_final_periodo rechaza un
    // período de CURSADA.
    @Column(name = "periodo_id", nullable = false, updatable = false)
    private Long periodoId;

    // fecha, horaInicio y horaFin cambian juntas (CHECK chk_horario_final_valido): sólo con reprogramar.
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    // aula es del módulo comisiones: se referencia por id. NULL si la carrera es virtual.
    @Column(name = "aula_id")
    @Setter
    private Long aulaId;

    // El tribunal. persona es del módulo personas: se referencia por id.
    @ElementCollection
    @CollectionTable(name = "examen_final_docente", joinColumns = @JoinColumn(name = "examen_final_id"))
    @Column(name = "docente_id", nullable = false)
    @Getter(AccessLevel.NONE)
    private Set<Long> docenteIds = new HashSet<>();

    public ExamenFinal(Long materiaId, Long periodoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                       Long aulaId) {
        this.materiaId = materiaId;
        this.periodoId = periodoId;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.aulaId = aulaId;
    }

    public Set<Long> getDocenteIds() {
        return Collections.unmodifiableSet(docenteIds);
    }

    public void reprogramar(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    /** La modificación reemplaza el tribunal completo. */
    public void cambiarDocentes(Collection<Long> docenteIds) {
        this.docenteIds.clear();
        this.docenteIds.addAll(docenteIds);
    }
}
