package com.utn.atenea.examenes.repository;

import com.utn.atenea.examenes.model.EvaluacionCursada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface EvaluacionCursadaRepository extends JpaRepository<EvaluacionCursada, Long> {

    // Las evaluaciones de una materia de comisión, en el orden en que se toman.
    List<EvaluacionCursada> findByComisionMateriaIdOrderByFechaAscIdAsc(Long comisionMateriaId);

    List<EvaluacionCursada> findByComisionMateriaIdInOrderByFechaAscIdAsc(Collection<Long> comisionMateriaIds);

    /**
     * El UNIQUE (comision_materia_id, nombre) de la tabla. excluirEvaluacionId es la que se modifica
     * (null en el alta).
     */
    @Query("""
            select count(e) > 0 from EvaluacionCursada e
            where e.comisionMateriaId = :comisionMateriaId
              and e.nombre = :nombre
              and (:excluirEvaluacionId is null or e.id <> :excluirEvaluacionId)
            """)
    boolean nombreEnUso(Long comisionMateriaId, String nombre, Long excluirEvaluacionId);
}
