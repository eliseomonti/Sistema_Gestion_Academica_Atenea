package com.utn.atenea.examenes.repository;

import com.utn.atenea.examenes.model.CierreCursada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Las actas sólo se insertan. La vigente de una cursada es la que ninguna otra rectifica. */
public interface CierreCursadaRepository extends JpaRepository<CierreCursada, Long> {

    @Query("""
            select c from CierreCursada c
            where c.inscripcionComisionId = :inscripcionComisionId
              and not exists (select r from CierreCursada r where r.cierreRectificado = c)
            """)
    Optional<CierreCursada> vigente(Long inscripcionComisionId);

    /** Las vigentes de varias cursadas en una consulta. Las que no tienen acta no vienen. */
    @Query("""
            select c from CierreCursada c
            where c.inscripcionComisionId in :inscripcionComisionIds
              and not exists (select r from CierreCursada r where r.cierreRectificado = c)
            """)
    List<CierreCursada> vigentes(Collection<Long> inscripcionComisionIds);
}
