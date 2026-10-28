package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.RolDocente;
import jakarta.validation.constraints.NotNull;

public record DocenteAsignadoRequest(

        @NotNull(message = "El docente es obligatorio")
        Long docenteId,

        @NotNull(message = "El rol del docente es obligatorio")
        RolDocente rol
) {
}
