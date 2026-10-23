package com.utn.atenea.personas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "persona")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido;

    @Column(nullable = false, unique = true, length = 10)
    private String dni;

    @Column(nullable = false, length = 50)
    private String telefono;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "legajo_alumno", unique = true)
    @Setter(AccessLevel.NONE)
    private Long legajoAlumno;

    @Column(name = "legajo_docente", unique = true)
    @Setter(AccessLevel.NONE)
    private Long legajoDocente;

    @Column(name = "legajo_admin", unique = true)
    @Setter(AccessLevel.NONE)
    private Long legajoAdmin;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean activo = true;

    @Column(name = "fecha_baja")
    @Setter(AccessLevel.NONE)
    private LocalDate fechaBaja;

    public Persona(String nombre, String apellido, String dni, String telefono,
                   LocalDate fechaNacimiento, String email) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.email = email;
    }

    public boolean esAlumno() {
        return legajoAlumno != null;
    }

    public boolean esDocente() {
        return legajoDocente != null;
    }

    public boolean esAdmin() {
        return legajoAdmin != null;
    }

    public void asignarLegajoAlumno(Long legajo) {
        if (esAlumno()) {
            throw new IllegalStateException("La persona " + id + " ya tiene legajo de alumno");
        }
        this.legajoAlumno = legajo;
    }

    public void asignarLegajoDocente(Long legajo) {
        if (esDocente()) {
            throw new IllegalStateException("La persona " + id + " ya tiene legajo de docente");
        }
        this.legajoDocente = legajo;
    }

    public void darDeBaja(LocalDate fecha) {
        this.activo = false;
        this.fechaBaja = fecha;
    }

    public void reactivar() {
        this.activo = true;
        this.fechaBaja = null;
    }
}
