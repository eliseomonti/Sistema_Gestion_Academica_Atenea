package com.utn.atenea.examenes;

/**
 * Una inscripción a una mesa de final que no se dio de baja. planEstudioId es el de la inscripción a
 * carrera con que se anotó: una materia puede estar en varios planes. estudianteId es su id de persona.
 */
public record InscriptoAFinal(Long inscripcionExamenFinalId, Long examenFinalId, Long planEstudioId,
                              Long estudianteId) {
}
