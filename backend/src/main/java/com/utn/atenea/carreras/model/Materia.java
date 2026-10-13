package com.utn.atenea.carreras.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "materia")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "carga_horaria_semanal", nullable = false)
    private int cargaHorariaSemanal;

    @Column(name = "carga_horaria_total", nullable = false)
    private int cargaHorariaTotal;

    public Materia(String nombre, int cargaHorariaSemanal, int cargaHorariaTotal) {
        this.nombre = nombre;
        this.cargaHorariaSemanal = cargaHorariaSemanal;
        this.cargaHorariaTotal = cargaHorariaTotal;
    }
}
