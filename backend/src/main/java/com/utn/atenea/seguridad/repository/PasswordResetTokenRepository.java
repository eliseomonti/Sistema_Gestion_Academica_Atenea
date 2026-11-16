package com.utn.atenea.seguridad.repository;

import com.utn.atenea.seguridad.model.PasswordResetToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<PasswordResetToken> findByToken(String tokenHash);

    List<PasswordResetToken> findByUsuarioIdAndFechaUsoIsNull(Long usuarioId);

    long countByUsuarioIdAndFechaCreacionAfter(Long usuarioId, LocalDateTime desde);
}
