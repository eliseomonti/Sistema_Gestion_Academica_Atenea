package com.utn.atenea.examenes;

/** Una inscripción a una materia de una comisión que no se dio de baja. estudianteId es su id de persona. */
public record InscriptoACursada(Long inscripcionComisionId, Long comisionMateriaId, Long estudianteId) {
}
