package com.utn.atenea.examenes.repository;

import com.utn.atenea.examenes.model.NotaFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Las notas sólo se insertan. La vigente de una inscripción es la que ninguna otra rectifica. */
public interface NotaFinalRepository extends JpaRepository<NotaFinal, Long> {

    @Query("""
            select n from NotaFinal n
            where n.inscripcionExamenFinalId = :inscripcionExamenFinalId
              and not exists (select r from NotaFinal r where r.notaRectificada = n)
            """)
    Optional<NotaFinal> vigente(Long inscripcionExamenFinalId);

    /** Las vigentes de varias inscripciones en una consulta. Las que no tienen nota no vienen. */
    @Query("""
            select n from NotaFinal n
            where n.inscripcionExamenFinalId in :inscripcionExamenFinalIds
              and not exists (select r from NotaFinal r where r.notaRectificada = n)
            """)
    List<NotaFinal> vigentes(Collection<Long> inscripcionExamenFinalIds);
}
