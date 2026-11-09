package com.utn.atenea.personas.dto;

/**
 * Si una persona se puede inscribir en un plan, sin inscribirla. La ventana de inscripción lo pide
 * antes de ofrecer el botón. {@code motivo} es null cuando se puede.
 */
public record VerificacionInscripcionResponse(
        boolean puedeInscribirse,
        String motivo
) {

    public static VerificacionInscripcionResponse permitida() {
        return new VerificacionInscripcionResponse(true, null);
    }

    public static VerificacionInscripcionResponse rechazada(String motivo) {
        return new VerificacionInscripcionResponse(false, motivo);
    }
}
