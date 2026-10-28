package com.utn.atenea.examenes.repository;

import com.utn.atenea.examenes.model.NotaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Las notas sólo se insertan. La vigente de un estudiante en una evaluación es la que ninguna otra
 * rectifica.
 */
public interface NotaEvaluacionRepository extends JpaRepository<NotaEvaluacion, Long> {

    @Query("""
            select n from NotaEvaluacion n
            where n.evaluacion.id = :evaluacionId
              and n.inscripcionComisionId = :inscripcionComisionId
              and not exists (select r from NotaEvaluacion r where r.notaRectificada = n)
            """)
    Optional<NotaEvaluacion> vigente(Long evaluacionId, Long inscripcionComisionId);

    // La planilla de una evaluación.
    @Query("""
            select n from NotaEvaluacion n
            where n.evaluacion.id = :evaluacionId
              and not exists (select r from NotaEvaluacion r where r.notaRectificada = n)
            """)
    List<NotaEvaluacion> vigentesDeLaEvaluacion(Long evaluacionId);

    /** Todas las vigentes de esas inscripciones, con su evaluación, en el orden en que se tomaron. */
    @Query("""
            select n from NotaEvaluacion n join fetch n.evaluacion e
            where n.inscripcionComisionId in :inscripcionComisionIds
              and not exists (select r from NotaEvaluacion r where r.notaRectificada = n)
            order by e.fecha, e.id
            """)
    List<NotaEvaluacion> vigentes(Collection<Long> inscripcionComisionIds);
}
