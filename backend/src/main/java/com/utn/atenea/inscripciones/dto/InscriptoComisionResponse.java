package com.utn.atenea.inscripciones.dto;

import java.time.LocalDate;

public record InscriptoComisionResponse(
        Long inscripcionId,
        Long personaId,
        Long legajo,
        String apellido,
        String nombre,
        LocalDate fechaInscripcion
) {

}