package com.utn.atenea.comisiones.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Agrega una materia del plan a la comisión. El período no se manda: es el de la materia en el plan.
 * docentes y horarios pueden ir vacíos y completarse después con la modificación.
 */
public record AltaComisionMateriaRequest(

        @NotNull(message = "La materia es obligatoria")
        Long materiaId,

        // null = sin límite de cupo.
        @Positive(message = "El cupo tiene que ser mayor a 0")
        Integer cupoMaximo,

        @NotNull(message = "La lista de docentes es obligatoria (puede ir vacía)")
        List<@NotNull(message = "El docente no puede ser null") @Valid DocenteAsignadoRequest> docentes,

        @NotNull(message = "La lista de horarios es obligatoria (puede ir vacía)")
        List<@NotNull(message = "El horario no puede ser null") @Valid HorarioRequest> horarios
) {
}
