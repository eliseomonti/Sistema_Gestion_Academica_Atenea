package com.utn.atenea.comisiones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.model.Turno;
import jakarta.validation.constraints.NotNull;

/**
 * Filtros del listado, como query params. Sólo la carrera es obligatoria; planEstudioId tiene que
 * ser de esa carrera. Sin anioLectivo se toma el año actual y sin activa, las activas.
 */
public record ComisionFiltroRequest(

        @NotNull(message = "La carrera es obligatoria")
        Long carreraId,

        Long planEstudioId,
        Integer anioLectivo,
        Integer anioCursado,
        PeriodoLectivo periodoLectivo,
        Turno turno,
        Boolean activa
) {
}
