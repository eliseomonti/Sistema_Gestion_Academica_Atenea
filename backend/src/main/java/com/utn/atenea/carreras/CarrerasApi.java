package com.utn.atenea.carreras;

import java.util.List;
import java.util.Optional;

public interface CarrerasApi {

    /** El plan con ese id, o vacío si no existe. */
    Optional<PlanEstudioResumen> buscarPlan(Long planEstudioId);

    /** Los ids de todos los planes de la carrera, vigentes o no. */
    List<Long> idsDePlanes(Long carreraId);
}
