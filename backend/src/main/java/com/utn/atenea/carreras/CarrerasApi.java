package com.utn.atenea.carreras;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CarrerasApi {

    /** La carrera existe, activa o dada de baja. */
    boolean existeCarrera(Long carreraId);

    /** El plan con ese id, o vacío si no existe. */
    Optional<PlanEstudioResumen> buscarPlan(Long planEstudioId);

    /** Los ids de todos los planes de la carrera, vigentes o no. */
    List<Long> idsDePlanes(Long carreraId);

    /** Cantidad de años del plan: el mayor año de cursado de sus materias, o 0 si no tiene ninguna. */
    int cantidadDeAnios(Long planEstudioId);

    /** La materia dentro del plan, o vacío si la materia no es de ese plan. */
    Optional<MateriaDelPlan> buscarMateriaDelPlan(Long planEstudioId, Long materiaId);

    /** Todas las materias del plan, ordenadas por año, período y nombre. */
    List<MateriaDelPlan> materiasDelPlan(Long planEstudioId);

    /** Las materias con esos ids. Las que no existen no vienen. */
    List<MateriaResumen> buscarMaterias(Collection<Long> materiaIds);

    /** Todas las correlatividades del plan. */
    List<CorrelativaDelPlan> correlatividades(Long planEstudioId);
}
