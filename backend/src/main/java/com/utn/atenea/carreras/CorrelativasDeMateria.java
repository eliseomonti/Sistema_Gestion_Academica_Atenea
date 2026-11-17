package com.utn.atenea.carreras;

import java.util.List;

/**
 * Una fila del régimen de correlatividades del plan: la materia y los códigos de las correlativas que
 * exige, separados por para qué (cursar o rendir) y en qué estado (cursada o aprobada).
 */
public record CorrelativasDeMateria(
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
