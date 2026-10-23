package com.utn.atenea.personas.repository;

import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.model.InscripcionCarrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionCarreraRepository extends JpaRepository<InscripcionCarrera, Long> {

    // Con el alumno en la condición, una inscripción no se encuentra desde la URL de otra persona.
    Optional<InscripcionCarrera> findByIdAndAlumnoId(Long id, Long alumnoId);

    List<InscripcionCarrera> findByAlumnoIdAndEstado(Long alumnoId, EstadoInscripcionCarrera estado);

    boolean existsByAlumnoIdAndPlanEstudioId(Long alumnoId, Long planEstudioId);

    boolean existsByAlumnoIdAndPlanEstudioIdInAndEstado(
            Long alumnoId, Collection<Long> planEstudioIds, EstadoInscripcionCarrera estado);
}
