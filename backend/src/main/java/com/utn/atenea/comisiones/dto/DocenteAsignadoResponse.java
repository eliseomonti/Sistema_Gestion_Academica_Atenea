package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.model.RolDocente;

public record DocenteAsignadoResponse(
        Long docenteId,
        String apellido,
        String nombre,
        RolDocente rol
) {
}
