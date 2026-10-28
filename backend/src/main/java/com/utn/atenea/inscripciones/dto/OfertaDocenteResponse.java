package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.comisiones.RolDocente;

public record OfertaDocenteResponse(
        String apellido,
        String nombre,
        RolDocente rol
) {
}
