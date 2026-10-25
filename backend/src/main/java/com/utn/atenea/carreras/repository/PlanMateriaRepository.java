package com.utn.atenea.carreras.repository;

import com.utn.atenea.carreras.model.PlanMateria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlanMateriaRepository extends JpaRepository<PlanMateria, Long> {

    // Con anioCursado null trae las de todos los años. La materia viene en la misma consulta.
    @Query("""
            select pm from PlanMateria pm join fetch pm.materia m
            where pm.planEstudio.id = :planEstudioId
              and (:anioCursado is null or pm.anioCursado = :anioCursado)
            order by pm.anioCursado, pm.periodoLectivo, m.nombre
            """)
    List<PlanMateria> buscarDelPlan(Long planEstudioId, Integer anioCursado);

    @EntityGraph(attributePaths = "materia")
    Optional<PlanMateria> findByPlanEstudioIdAndMateriaId(Long planEstudioId, Long materiaId);

    @Query("select coalesce(max(pm.anioCursado), 0) from PlanMateria pm where pm.planEstudio.id = :planEstudioId")
    int cantidadDeAnios(Long planEstudioId);
}
