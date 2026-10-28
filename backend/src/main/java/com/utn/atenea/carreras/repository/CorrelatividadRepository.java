package com.utn.atenea.carreras.repository;

import com.utn.atenea.carreras.model.Correlatividad;
import com.utn.atenea.carreras.model.CorrelatividadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CorrelatividadRepository extends JpaRepository<Correlatividad, CorrelatividadId> {

    // Las dos materias vienen en la misma consulta.
    @Query("""
            select c from Correlatividad c
            join fetch c.planMateria pm join fetch pm.materia
            join fetch c.correlativa co join fetch co.materia
            where c.planEstudioId = :planEstudioId
            """)
    List<Correlatividad> buscarDelPlan(Long planEstudioId);
}
