package com.utn.atenea.personas.repository;

import com.utn.atenea.personas.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long>, JpaSpecificationExecutor<Persona> {

    Optional<Persona> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    // Al modificar: el DNI o el email están en uso por otra persona que no es la que se modifica.
    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Persona> findByIdInAndLegajoDocenteIsNotNull(Collection<Long> ids);

    List<Persona> findByIdInAndLegajoEstudianteIsNotNull(Collection<Long> ids);

    @Query("select coalesce(max(p.legajoEstudiante), 0L) from Persona p")
    Long ultimoLegajoEstudiante();

    @Query("select coalesce(max(p.legajoDocente), 0L) from Persona p")
    Long ultimoLegajoDocente();
}
