package com.utn.atenea.seguridad.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "persona_id", nullable = false, unique = true, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long personaId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    // La columna en la bdd es TINYINT: sin @JdbcTypeCode, Hibernate espera INTEGER y el validate falla.
    @Column(name = "intentos_fallidos", nullable = false)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int intentosFallidos = 0;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_cambio_password")
    private LocalDateTime fechaCambioPassword;

    @ManyToMany
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    @Setter(AccessLevel.NONE)
    private Set<Rol> roles = new HashSet<>();

    public Usuario(Long personaId, String username, String passwordHash, LocalDateTime fechaCreacion) {
        this.personaId = personaId;
        this.username = username;
        this.password = passwordHash;
        this.fechaCreacion = fechaCreacion;
    }

    public void agregarRol(Rol rol) {
        roles.add(rol);
    }

    public void quitarRol(Rol rol) {
        roles.remove(rol);
    }
}
