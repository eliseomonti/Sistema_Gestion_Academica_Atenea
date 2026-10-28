package com.utn.atenea.examenes.repository;

import com.utn.atenea.examenes.model.ExamenFinal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ExamenFinalRepository extends JpaRepository<ExamenFinal, Long> {

    // Las mesas de un turno, con su tribunal.
    @EntityGraph(attributePaths = "docenteIds")
    List<ExamenFinal> findByPeriodoIdOrderByFechaAscHoraInicioAsc(Long periodoId);

    @EntityGraph(attributePaths = "docenteIds")
    List<ExamenFinal> findByPeriodoIdInOrderByFechaAscHoraInicioAsc(Collection<Long> periodoIds);

    @EntityGraph(attributePaths = "docenteIds")
    List<ExamenFinal> findByIdIn(Collection<Long> ids);

    /** Las mesas en cuyo tribunal está el docente, entre esas fechas (inclusive), por fecha y hora. */
    @Query("""
            select e from ExamenFinal e
            where :docenteId member of e.docenteIds
              and e.fecha between :desde and :hasta
            order by e.fecha, e.horaInicio
            """)
    List<ExamenFinal> delTribunal(Long docenteId, LocalDate desde, LocalDate hasta);

    /**
     * El UNIQUE (materia_id, fecha, hora_inicio) de la tabla. excluirExamenFinalId es la mesa que se
     * modifica (null en el alta).
     */
    @Query("""
            select count(e) > 0 from ExamenFinal e
            where e.materiaId = :materiaId
              and e.fecha = :fecha
              and e.horaInicio = :horaInicio
              and (:excluirExamenFinalId is null or e.id <> :excluirExamenFinalId)
            """)
    boolean horarioEnUso(Long materiaId, LocalDate fecha, LocalTime horaInicio, Long excluirExamenFinalId);

    /**
     * Otra mesa usa el aula ese día en una franja que se pisa con [horaInicio, horaFin). Las que se
     * tocan no chocan. excluirExamenFinalId es la mesa que se modifica (null en el alta).
     */
    @Query("""
            select count(e) > 0 from ExamenFinal e
            where e.aulaId = :aulaId
              and e.fecha = :fecha
              and e.horaInicio < :horaFin and e.horaFin > :horaInicio
              and (:excluirExamenFinalId is null or e.id <> :excluirExamenFinalId)
            """)
    boolean aulaOcupada(Long aulaId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                        Long excluirExamenFinalId);
}
