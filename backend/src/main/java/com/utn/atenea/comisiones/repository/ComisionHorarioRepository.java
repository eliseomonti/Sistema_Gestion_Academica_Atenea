package com.utn.atenea.comisiones.repository;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.model.ComisionHorario;
import com.utn.atenea.comisiones.model.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * Sólo para consultas: los horarios se crean y se borran desde ComisionMateria.
 *
 * Las dos consultas buscan choques con una franja [horaInicio, horaFin) de ese día, en materias de
 * comisiones activas del mismo año lectivo cuyos períodos se pisan con el de la materia (ver
 * PeriodoLectivo.superpuestos). excluirComisionMateriaId es la materia que se modifica (null en el
 * alta): sus horarios actuales se van a reemplazar, así que no cuentan.
 */
public interface ComisionHorarioRepository extends JpaRepository<ComisionHorario, Long> {

    @Query("""
            select count(h) > 0 from ComisionHorario h
            where h.aula.id = :aulaId
              and h.diaSemana = :diaSemana
              and h.horaInicio < :horaFin and h.horaFin > :horaInicio
              and h.comisionMateria.comision.activa = true
              and h.comisionMateria.comision.anioLectivo = :anioLectivo
              and h.comisionMateria.periodoLectivo in :periodos
              and (:excluirComisionMateriaId is null or h.comisionMateria.id <> :excluirComisionMateriaId)
            """)
    boolean aulaOcupada(Long aulaId, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin,
                        int anioLectivo, Collection<PeriodoLectivo> periodos, Long excluirComisionMateriaId);

    /** De esos docentes, los que ya dan clase en esa franja en otra materia. */
    @Query("""
            select distinct d.docenteId from ComisionHorario h join h.comisionMateria cm join cm.docentes d
            where d.docenteId in :docenteIds
              and h.diaSemana = :diaSemana
              and h.horaInicio < :horaFin and h.horaFin > :horaInicio
              and cm.comision.activa = true
              and cm.comision.anioLectivo = :anioLectivo
              and cm.periodoLectivo in :periodos
              and (:excluirComisionMateriaId is null or cm.id <> :excluirComisionMateriaId)
            """)
    List<Long> docentesOcupados(Collection<Long> docenteIds, DiaSemana diaSemana, LocalTime horaInicio,
                                LocalTime horaFin, int anioLectivo, Collection<PeriodoLectivo> periodos,
                                Long excluirComisionMateriaId);
}
