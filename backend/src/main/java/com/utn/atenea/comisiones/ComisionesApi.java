package com.utn.atenea.comisiones;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComisionesApi {

    /**
     * La materia de la comisión, bloqueada (SELECT ... FOR UPDATE) hasta que termine la transacción
     * de quien llama, o vacío si no existe. Serializa las inscripciones a la misma materia: sin el
     * bloqueo, dos estudiantes que se anotan a la vez al último lugar contarían el mismo cupo libre.
     */
    Optional<ComisionMateriaResumen> bloquearComisionMateria(Long comisionMateriaId);

    /** Las materias de comisión con esos ids. Las que no existen no vienen. */
    List<ComisionMateriaResumen> buscarComisionesMateria(Collection<Long> comisionMateriaIds);

    /** Los ids de las materias de comisión en que se dicta esa materia en ese año lectivo, en cualquier comisión. */
    List<Long> idsDeComisionesMateria(Long materiaId, int anioLectivo);

    /** Las materias que se dictan en las comisiones activas del plan en ese año lectivo, con horarios y docentes. */
    List<MateriaDictada> materiasDictadas(Long planEstudioId, int anioLectivo);

    /** El docente está asignado a la materia de la comisión, con cualquier rol. */
    boolean esDocenteDe(Long docenteId, Long comisionMateriaId);

    /** Las aulas con esos ids, activas o dadas de baja (activa lo distingue). Las que no existen no vienen. */
    List<AulaResumen> buscarAulas(Collection<Long> aulaIds);
}
