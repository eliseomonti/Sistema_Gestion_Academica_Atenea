package com.utn.atenea.comisiones.repository;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.model.Comision;
import com.utn.atenea.comisiones.Turno;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ComisionRepository extends JpaRepository<Comision, Long> {

    /**
     * Ya hay otra comisión con ese número en el mismo plan, año de la carrera y año lectivo (el
     * UNIQUE de comision). excluirComisionId es la que se modifica (null en el alta), para que no
     * choque consigo misma.
     */
    @Query("""
            select count(c) > 0 from Comision c
            where c.planEstudioId = :planEstudioId
              and c.anioCursado = :anioCursado
              and c.nroComision = :nroComision
              and c.anioLectivo = :anioLectivo
              and (:excluirComisionId is null or c.id <> :excluirComisionId)
            """)
    boolean numeroEnUso(Long planEstudioId, int anioCursado, int nroComision, int anioLectivo,
                        Long excluirComisionId);

    /**
     * Listado del admin. anioCursado, turno y periodoLectivo en null no filtran; periodoLectivo
     * trae las comisiones con al menos una materia de ese período. Las materias vienen en la misma
     * consulta porque el listado muestra cuántas tiene cada una.
     */
    @EntityGraph(attributePaths = "materias")
    @Query("""
            select c from Comision c
            where c.planEstudioId in :planesEstudioIds
              and c.anioLectivo = :anioLectivo
              and c.activa = :activa
              and (:anioCursado is null or c.anioCursado = :anioCursado)
              and (:turno is null or c.turno = :turno)
              and (:periodoLectivo is null or exists (
                    select 1 from ComisionMateria cm
                    where cm.comision = c and cm.periodoLectivo = :periodoLectivo))
            order by c.anioCursado, c.nroComision, c.planEstudioId
            """)
    List<Comision> listar(Collection<Long> planesEstudioIds, int anioLectivo, boolean activa,
                          Integer anioCursado, Turno turno, PeriodoLectivo periodoLectivo);
}
