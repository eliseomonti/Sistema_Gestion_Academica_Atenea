package com.utn.atenea.seguridad.repository;

import com.utn.atenea.seguridad.model.NombreRol;
import com.utn.atenea.seguridad.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(NombreRol nombre);
}
