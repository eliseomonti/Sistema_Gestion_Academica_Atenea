package com.utn.atenea.comisiones.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Reemplaza el cupo, los docentes y los horarios de la materia: lo que no venga se quita. La materia
 * no cambia; para eso se quita y se agrega otra.
 */
public record ModificacionComisionMateriaRequest(

        // null = sin límite de cupo.
        @Positive(message = "El cupo tiene que ser mayor a 0")
        Integer cupoMaximo,

        @NotNull(message = "La lista de docentes es obligatoria (puede ir vacía)")
        List<@NotNull(message = "El docente no puede ser null") @Valid DocenteAsignadoRequest> docentes,

        @NotNull(message = "La lista de horarios es obligatoria (puede ir vacía)")
        List<@NotNull(message = "El horario no puede ser null") @Valid HorarioRequest> horarios
) {
}
