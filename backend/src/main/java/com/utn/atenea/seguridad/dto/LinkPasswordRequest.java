package com.utn.atenea.seguridad.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Pedido del link desde Mi cuenta. El rol es con el que entró: va en el link del mail para que, al
 * terminar, la persona vuelva a ese login. Es opcional.
 */
public record LinkPasswordRequest(@Pattern(regexp = ROLES) String rol) {

    /** Los valores que acepta el rol, tal como aparecen en la URL del login de Angular. */
    public static final String ROLES = "admin|docente|estudiante";
}
