package com.utn.atenea.comisiones.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "aula")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    // NULL si no está relevada.
    private Integer capacidad;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean activa = true;

    @Column(name = "fecha_baja")
    @Setter(AccessLevel.NONE)
    private LocalDate fechaBaja;

    public Aula(String nombre, Integer capacidad) {
        this.nombre = nombre;
        this.capacidad = capacidad;
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
