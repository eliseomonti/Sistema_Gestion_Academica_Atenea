package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;

import java.util.List;

/** Una fila del régimen de correlatividades del plan del estudiante: la materia y los códigos de sus correlativas. */
public record CorrelatividadesMateriaEstudianteResponse(
        Long materiaId,
        String codigo,
        String nombre,
        int anioCursado,
        PeriodoLectivo periodoLectivo,
        List<String> cursadasParaCursar,
        List<String> aprobadasParaCursar,
        List<String> cursadasParaRendir,
        List<String> aprobadasParaRendir
) {
}
