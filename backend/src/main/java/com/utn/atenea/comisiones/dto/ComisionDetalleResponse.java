package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.model.Turno;

import java.time.LocalDate;
import java.util.List;

/** La comisión con todo lo que se dicta en ella: con esto Angular arma la grilla de la planilla. */
public record ComisionDetalleResponse(
        Long id,
        String nombre,
        Long planEstudioId,
        int anioCursado,
        int nroComision,
        Turno turno,
        int anioLectivo,
        boolean activa,
        LocalDate fechaBaja,
        List<ComisionMateriaResponse> materias
) {
}
