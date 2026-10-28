package com.utn.atenea.examenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Las condiciones que el docente cierra o corrige en el acta. Se puede mandar una parte: las
 * inscripciones que no vienen quedan como estaban.
 */
public record CargaCierreRequest(

        @NotEmpty(message = "Tiene que venir al menos una inscripción")
        List<@Valid CierreRequest> cierres
) {
}
