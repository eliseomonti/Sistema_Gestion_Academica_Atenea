package com.utn.atenea.comisiones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;

import java.util.List;

/** Una materia de la comisión: parte del detalle y respuesta del alta y la modificación de la materia. */
public record ComisionMateriaResponse(
        Long id,
        Long materiaId,
        String nombreMateria,
        PeriodoLectivo periodoLectivo,
        Integer cupoMaximo,
        List<DocenteAsignadoResponse> docentes,
        List<HorarioResponse> horarios
) {
}
