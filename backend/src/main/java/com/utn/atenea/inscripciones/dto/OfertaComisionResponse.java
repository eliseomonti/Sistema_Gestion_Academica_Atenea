package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.comisiones.Turno;

import java.util.List;

/**
 * Una comisión en que se dicta la materia. cupoMaximo y cupoDisponible vienen en null si no hay límite;
 * inscripto dice si el estudiante ya está anotado en esta.
 */
public record OfertaComisionResponse(
        Long comisionMateriaId,
        String nombreComision,
        Turno turno,
        List<OfertaHorarioResponse> horarios,
        List<OfertaDocenteResponse> docentes,
        Integer cupoMaximo,
        Integer cupoDisponible,
        boolean inscripto
) {
}
