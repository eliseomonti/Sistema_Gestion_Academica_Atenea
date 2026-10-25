package com.utn.atenea.comisiones.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "comision_horario")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComisionHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comision_materia_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private ComisionMateria comisionMateria;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    // NULL si la cursada es virtual.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    // Se crea desde ComisionMateria.agregarHorario, que mantiene los dos lados de la relación.
    ComisionHorario(ComisionMateria comisionMateria, DiaSemana diaSemana, LocalTime horaInicio,
                    LocalTime horaFin, Aula aula) {
        this.comisionMateria = comisionMateria;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.aula = aula;
    }

    /** Mismo día y franjas que se pisan. Las que se tocan no chocan: 8 a 10 y 10 a 12 conviven. */
    public boolean seSuperponeCon(DiaSemana dia, LocalTime inicio, LocalTime fin) {
        return diaSemana == dia && horaInicio.isBefore(fin) && horaFin.isAfter(inicio);
    }
}
