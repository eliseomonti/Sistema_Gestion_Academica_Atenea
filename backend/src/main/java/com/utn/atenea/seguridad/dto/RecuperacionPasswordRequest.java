package com.utn.atenea.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * El email de la persona. El rol es el de la pantalla de login desde la que se pidió: va en el link
 * del mail para que, al terminar, la persona vuelva a ese login. Es opcional.
 */
public record RecuperacionPasswordRequest(
        @NotBlank @Email String email,
        @Pattern(regexp = LinkPasswordRequest.ROLES) String rol) {
}
