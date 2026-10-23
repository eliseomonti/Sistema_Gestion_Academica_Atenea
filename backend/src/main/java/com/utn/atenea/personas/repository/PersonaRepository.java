package com.utn.atenea.personas.repository;

import com.utn.atenea.personas.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long>, JpaSpecificationExecutor<Persona> {

    Optional<Persona> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    // Al modificar: el DNI o el email están en uso por otra persona que no es la que se modifica.
    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("select coalesce(max(p.legajoAlumno), 0L) from Persona p")
    Long ultimoLegajoAlumno();

    @Query("select coalesce(max(p.legajoDocente), 0L) from Persona p")
    Long ultimoLegajoDocente();
}
