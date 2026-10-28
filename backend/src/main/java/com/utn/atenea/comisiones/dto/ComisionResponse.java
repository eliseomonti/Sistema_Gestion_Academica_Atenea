package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.Turno;

/** Fila del listado de comisiones; también es la respuesta del alta, la modificación y la baja. */
public record ComisionResponse(
        Long id,
        String nombre,
        Long planEstudioId,
        int anioCursado,
        int nroComision,
        Turno turno,
        int anioLectivo,
        int cantidadMaterias,
        boolean activa
) {
}
