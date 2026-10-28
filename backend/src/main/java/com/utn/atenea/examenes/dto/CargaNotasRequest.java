package com.utn.atenea.examenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Las notas que el docente carga o corrige en una evaluación o una mesa. Se puede mandar una parte de
 * la planilla: las inscripciones que no vienen quedan como estaban.
 */
public record CargaNotasRequest(

        @NotEmpty(message = "Tiene que venir al menos una nota")
        List<@Valid NotaRequest> notas
) {
}
