package com.utn.atenea.inscripciones.repository;

import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionFinal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionFinalRepository extends JpaRepository<InscripcionFinal, Long> {

    // El UNIQUE de la tabla: si existe y está en BAJA, se reinscribe en lugar de crear otra.
    Optional<InscripcionFinal> findByInscripcionCarreraIdAndExamenFinalId(Long inscripcionCarreraId, Long examenFinalId);

    // inscripcionCarreraIds son las del estudiante que llama: así no se encuentra la inscripción de otro.
    Optional<InscripcionFinal> findByIdAndInscripcionCarreraIdIn(Long id, Collection<Long> inscripcionCarreraIds);

    List<InscripcionFinal> findByInscripcionCarreraIdAndEstado(Long inscripcionCarreraId, EstadoInscripcion estado);

    List<InscripcionFinal> findByInscripcionCarreraIdInAndEstado(
            Collection<Long> inscripcionCarreraIds, EstadoInscripcion estado);

    // Los anotados a la mesa, para el acta.
    List<InscripcionFinal> findByExamenFinalIdAndEstado(Long examenFinalId, EstadoInscripcion estado);

    // Ya está anotado a otra mesa de la misma materia en el turno: examenFinalIds los da examenes.
    boolean existsByInscripcionCarreraIdAndExamenFinalIdInAndEstado(
            Long inscripcionCarreraId, Collection<Long> examenFinalIds, EstadoInscripcion estado);
}
