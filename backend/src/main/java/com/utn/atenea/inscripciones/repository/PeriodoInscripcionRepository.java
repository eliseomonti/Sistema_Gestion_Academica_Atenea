package com.utn.atenea.inscripciones.repository;

import com.utn.atenea.inscripciones.model.PeriodoInscripcion;
import com.utn.atenea.inscripciones.model.TipoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PeriodoInscripcionRepository extends JpaRepository<PeriodoInscripcion, Long> {

    /** Listado del admin. tipo en null no filtra. */
    @Query("""
            select p from PeriodoInscripcion p
            where p.carreraId = :carreraId
              and p.anioLectivo = :anioLectivo
              and (:tipo is null or p.tipo = :tipo)
            order by p.fechaInicio desc
            """)
    List<PeriodoInscripcion> listar(Long carreraId, int anioLectivo, TipoPeriodo tipo);

    /**
     * Ya hay otro período del mismo tipo en la carrera cuyas fechas [fechaInicio, fechaCierre) se pisan
     * con estas, habilitado o no. excluirPeriodoId es el que se modifica (null en el alta). Cubre también
     * el UNIQUE (carrera_id, tipo, fecha_inicio) de la tabla.
     */
    @Query("""
            select count(p) > 0 from PeriodoInscripcion p
            where p.carreraId = :carreraId
              and p.tipo = :tipo
              and p.fechaInicio < :fechaCierre and p.fechaCierre > :fechaInicio
              and (:excluirPeriodoId is null or p.id <> :excluirPeriodoId)
            """)
    boolean seSuperpone(Long carreraId, TipoPeriodo tipo, LocalDateTime fechaInicio, LocalDateTime fechaCierre,
                        Long excluirPeriodoId);

    /** Los períodos de ese tipo en los que la carrera acepta inscripciones ahora (ver PeriodoInscripcion.estaAbierto). */
    @Query("""
            select p from PeriodoInscripcion p
            where p.carreraId = :carreraId
              and p.tipo = :tipo
              and p.habilitada = true
              and p.fechaInicio <= :ahora and p.fechaCierre > :ahora
            order by p.fechaInicio
            """)
    List<PeriodoInscripcion> abiertos(Long carreraId, TipoPeriodo tipo, LocalDateTime ahora);
}
