package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.model.Turno;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Alta del grupo. Las materias se agregan después, de a una. */
public record AltaComisionRequest(

        @NotNull(message = "El plan de estudio es obligatorio")
        Long planEstudioId,

        @NotNull(message = "El año de la carrera es obligatorio")
        @Min(value = 1, message = "El año de la carrera tiene que estar entre 1 y 7")
        @Max(value = 7, message = "El año de la carrera tiene que estar entre 1 y 7")
        Integer anioCursado,

        @NotNull(message = "El número de comisión es obligatorio")
        @Positive(message = "El número de comisión tiene que ser mayor a 0")
        Integer nroComision,

        @NotNull(message = "El turno es obligatorio")
        Turno turno,

        @NotNull(message = "El año lectivo es obligatorio")
        @Min(value = 2000, message = "El año lectivo tiene que estar entre 2000 y 2100")
        @Max(value = 2100, message = "El año lectivo tiene que estar entre 2000 y 2100")
        Integer anioLectivo
) {
}
