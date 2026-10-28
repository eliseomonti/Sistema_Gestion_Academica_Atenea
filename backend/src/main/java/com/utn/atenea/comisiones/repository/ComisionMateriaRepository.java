package com.utn.atenea.comisiones.repository;

import com.utn.atenea.comisiones.model.ComisionMateria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Las materias se agregan y se quitan desde Comision; esto es para consultarlas. */
public interface ComisionMateriaRepository extends JpaRepository<ComisionMateria, Long> {

    // Con la comisión en la condición, una materia no se encuentra desde la URL de otra comisión.
    Optional<ComisionMateria> findByIdAndComisionId(Long id, Long comisionId);

    // El docente está asignado a la materia de la comisión, con cualquier rol.
    boolean existsByIdAndDocentesDocenteId(Long id, Long docenteId);

    // SELECT ... FOR UPDATE: ver ComisionesApi.bloquearComisionMateria.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cm from ComisionMateria cm join fetch cm.comision where cm.id = :id")
    Optional<ComisionMateria> bloquear(Long id);

    @EntityGraph(attributePaths = "comision")
    List<ComisionMateria> findByIdIn(Collection<Long> ids);

    @Query("select cm.id from ComisionMateria cm where cm.materiaId = :materiaId and cm.comision.anioLectivo = :anioLectivo")
    List<Long> idsPorMateriaYAnioLectivo(Long materiaId, int anioLectivo);

    // Una sola consulta con comisión, horarios, aulas y docentes: la oferta los muestra todos.
    @EntityGraph(attributePaths = {"comision", "horarios", "horarios.aula", "docentes"})
    @Query("""
            select cm from ComisionMateria cm
            where cm.comision.planEstudioId = :planEstudioId
              and cm.comision.anioLectivo = :anioLectivo
              and cm.comision.activa = true
            order by cm.comision.anioCursado, cm.comision.nroComision
            """)
    List<ComisionMateria> dictadasEnElPlan(Long planEstudioId, int anioLectivo);

    @Query("""
            select count(cm) > 0 from ComisionMateria cm join cm.docentes d
            where cm.id = :comisionMateriaId and d.docenteId = :docenteId
            """)
    boolean tieneAlDocente(Long comisionMateriaId, Long docenteId);

    /** Las que el docente tiene asignadas en comisiones activas de ese año lectivo, con horarios y aulas. */
    @EntityGraph(attributePaths = {"comision", "horarios", "horarios.aula", "docentes"})
    @Query("""
            select cm from ComisionMateria cm
            where cm.comision.anioLectivo = :anioLectivo
              and cm.comision.activa = true
              and exists (select 1 from ComisionMateria asignada join asignada.docentes d
                          where asignada.id = cm.id and d.docenteId = :docenteId)
            order by cm.periodoLectivo, cm.comision.anioCursado, cm.comision.nroComision
            """)
    List<ComisionMateria> asignadasAlDocente(Long docenteId, int anioLectivo);
}
