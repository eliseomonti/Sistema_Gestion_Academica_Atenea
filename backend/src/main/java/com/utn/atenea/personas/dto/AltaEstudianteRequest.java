package com.utn.atenea.personas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Alta de un estudiante: sus datos personales y el plan al que se inscribe. La carrera sale del plan. */
public record AltaEstudianteRequest(

        @NotNull(message = "Los datos personales son obligatorios")
        @Valid
        DatosPersonalesRequest datosPersonales,

        @NotNull(message = "El plan de estudio es obligatorio")
        Long planEstudioId
) {
}
