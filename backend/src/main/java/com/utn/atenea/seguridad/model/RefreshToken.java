package com.utn.atenea.seguridad.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

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

    @Column(nullable = false)
    private boolean revocado = false;

    public RefreshToken(Usuario usuario, String tokenHash, LocalDateTime fechaCreacion, LocalDateTime expiracion) {
        this.usuario = usuario;
        this.token = tokenHash;
        this.fechaCreacion = fechaCreacion;
        this.expiracion = expiracion;
    }

    public void revocar() {
        this.revocado = true;
    }

    public boolean esValido(LocalDateTime ahora) {
        return !revocado && ahora.isBefore(expiracion);
    }
}
