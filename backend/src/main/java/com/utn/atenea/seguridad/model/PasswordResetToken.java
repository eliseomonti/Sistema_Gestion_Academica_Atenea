package com.utn.atenea.seguridad.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(nullable = false, unique = true, updatable = false)
    private String token;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiracion;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    public PasswordResetToken(Usuario usuario, String tokenHash, LocalDateTime fechaCreacion, LocalDateTime expiracion) {
        this.usuario = usuario;
        this.token = tokenHash;
        this.fechaCreacion = fechaCreacion;
        this.expiracion = expiracion;
    }

    public boolean estaUsado() {
        return fechaUso != null;
    }

    public boolean esValido(LocalDateTime ahora) {
        return !estaUsado() && ahora.isBefore(expiracion);
    }

    public void marcarUsado(LocalDateTime ahora) {
        this.fechaUso = ahora;
    }
}
