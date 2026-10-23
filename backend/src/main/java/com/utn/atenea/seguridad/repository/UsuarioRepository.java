package com.utn.atenea.seguridad.repository;

import com.utn.atenea.seguridad.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<Usuario> findByPersonaId(Long personaId);
}
