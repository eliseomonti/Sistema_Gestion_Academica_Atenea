package com.utn.atenea.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El máximo es el de BCrypt: sólo usa los primeros 72 bytes de la contraseña. */
public record CambioPasswordRequest(
        @NotBlank String passwordActual,
        @NotBlank @Size(min = 8, max = 72, message = "Tiene que tener entre 8 y 72 caracteres.") String passwordNueva) {
}
