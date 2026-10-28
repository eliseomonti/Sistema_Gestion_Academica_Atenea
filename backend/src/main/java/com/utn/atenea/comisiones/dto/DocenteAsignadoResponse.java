package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.RolDocente;

public record DocenteAsignadoResponse(
        Long docenteId,
        String apellido,
        String nombre,
        RolDocente rol
) {
}
