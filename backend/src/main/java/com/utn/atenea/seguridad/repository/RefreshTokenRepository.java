package com.utn.atenea.seguridad.repository;

import com.utn.atenea.seguridad.model.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.roles"})
    Optional<RefreshToken> findByToken(String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken t set t.revocado = true where t.usuario.id = :usuarioId and t.revocado = false")
    int revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId);
}
