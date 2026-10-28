package com.utn.atenea.inscripciones.repository;

import com.utn.atenea.inscripciones.model.EstadoInscripcion;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionComisionRepository extends JpaRepository<InscripcionComision, Long> {

    // El UNIQUE de la tabla: si existe y está en BAJA, se reinscribe en lugar de crear otra.
    Optional<InscripcionComision> findByInscripcionCarreraIdAndComisionMateriaId(
            Long inscripcionCarreraId, Long comisionMateriaId);

    // inscripcionCarreraIds son las del estudiante que llama: así no se encuentra la inscripción de otro.
    Optional<InscripcionComision> findByIdAndInscripcionCarreraIdIn(Long id, Collection<Long> inscripcionCarreraIds);

    List<InscripcionComision> findByInscripcionCarreraIdInAndEstado(
            Collection<Long> inscripcionCarreraIds, EstadoInscripcion estado);

    List<InscripcionComision> findByComisionMateriaIdAndEstado(Long comisionMateriaId, EstadoInscripcion estado);

    // Cupo ocupado: sólo cuentan las INSCRIPTO.
    long countByComisionMateriaIdAndEstado(Long comisionMateriaId, EstadoInscripcion estado);

    /** El cupo ocupado de varias materias de comisión en una consulta. Las que no tienen inscriptos no vienen. */
    @Query("""
            select new com.utn.atenea.inscripciones.repository.CantidadInscriptos(i.comisionMateriaId, count(i))
            from InscripcionComision i
            where i.comisionMateriaId in :comisionMateriaIds
              and i.estado = com.utn.atenea.inscripciones.model.EstadoInscripcion.INSCRIPTO
            group by i.comisionMateriaId
            """)
    List<CantidadInscriptos> contarInscriptos(Collection<Long> comisionMateriaIds);

    // Ya está anotado a la misma materia en otra comisión: comisionMateriaIds las da comisiones.
    boolean existsByInscripcionCarreraIdAndComisionMateriaIdInAndEstado(
            Long inscripcionCarreraId, Collection<Long> comisionMateriaIds, EstadoInscripcion estado);
}
