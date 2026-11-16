package com.utn.atenea.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El token es el que llegó en el link del mail. El máximo de la contraseña es el de BCrypt. */
public record RestablecimientoPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72, message = "Tiene que tener entre 8 y 72 caracteres.") String passwordNueva) {
}
