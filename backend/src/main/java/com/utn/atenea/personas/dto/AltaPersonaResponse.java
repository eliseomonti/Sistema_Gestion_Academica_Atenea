package com.utn.atenea.personas.dto;

/**
 * Respuesta del alta de un estudiante o docente. Es la única vez que viaja la contraseña temporal:
 * también se le manda por mail a la persona, y si el mail no sale (mailEnviado en false), el admin
 * la ve en pantalla para entregársela.
 */
public record AltaPersonaResponse(
        Long personaId,
        Long legajo,
        String username,
        String passwordTemporal,
        boolean mailEnviado
) {
}
