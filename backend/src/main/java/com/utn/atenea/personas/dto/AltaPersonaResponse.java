package com.utn.atenea.personas.dto;

/**
 * Respuesta del alta de un estudiante o docente. El usuario y la contraseña temporal no viajan acá:
 * le llegan sólo a la persona, por mail. Si el mail no sale (mailEnviado en false), la persona puede
 * pedir una contraseña nueva con su email desde "¿Olvidaste tu contraseña?".
 */
public record AltaPersonaResponse(
        Long personaId,
        Long legajo,
        boolean mailEnviado
) {
}
