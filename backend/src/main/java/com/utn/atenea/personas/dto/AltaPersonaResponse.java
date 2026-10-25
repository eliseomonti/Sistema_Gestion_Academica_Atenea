package com.utn.atenea.personas.dto;

/**
 * Respuesta del alta de un estudiante o docente. Es la única vez que viaja la contraseña temporal:
 * la pantalla se la muestra al admin para que se la entregue a la persona.
 */
public record AltaPersonaResponse(
        Long personaId,
        Long legajo,
        String username,
        String passwordTemporal
) {
}
