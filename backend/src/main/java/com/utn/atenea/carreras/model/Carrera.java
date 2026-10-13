package com.utn.atenea.carreras.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "carrera")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String duracion;

    @Column(name = "titulo_otorgado", nullable = false, length = 100)
    private String tituloOtorgado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_carrera", nullable = false)
    private ModalidadCarrera modalidad;

    // activa y fechaBaja cambian juntas (CHECK chk_carrera_baja): sólo con darDeBaja/reactivar.
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean activa = true;

    @Column(name = "fecha_baja")
    @Setter(AccessLevel.NONE)
    private LocalDate fechaBaja;

    public Carrera(String nombre, String duracion, String tituloOtorgado, ModalidadCarrera modalidad) {
        this.nombre = nombre;
        this.duracion = duracion;
        this.tituloOtorgado = tituloOtorgado;
        this.modalidad = modalidad;
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
