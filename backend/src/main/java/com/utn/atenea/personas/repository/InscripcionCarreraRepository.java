package com.utn.atenea.personas.repository;

import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.model.InscripcionCarrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionCarreraRepository extends JpaRepository<InscripcionCarrera, Long> {

    // Con el estudiante en la condición, una inscripción no se encuentra desde la URL de otra persona.
    Optional<InscripcionCarrera> findByIdAndEstudianteId(Long id, Long estudianteId);

    List<InscripcionCarrera> findByEstudianteId(Long estudianteId);

    List<InscripcionCarrera> findByEstudianteIdAndEstado(Long estudianteId, EstadoInscripcionCarrera estado);

    boolean existsByEstudianteIdAndPlanEstudioId(Long estudianteId, Long planEstudioId);

    boolean existsByEstudianteIdAndPlanEstudioIdInAndEstado(
            Long estudianteId, Collection<Long> planEstudioIds, EstadoInscripcionCarrera estado);
}
