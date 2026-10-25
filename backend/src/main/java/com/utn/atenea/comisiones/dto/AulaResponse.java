package com.utn.atenea.comisiones.dto;

/** Opción del desplegable de aulas, al cargar el horario de una materia. */
public record AulaResponse(
        Long id,
        String nombre,
        Integer capacidad
) {
}
