package com.utn.atenea.carreras.dto;

import com.utn.atenea.carreras.PeriodoLectivo;

import java.util.List;

/** Una fila de la tabla del régimen de correlatividades: la materia y los códigos de sus correlativas. */
public record CorrelatividadesMateriaResponse(
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
