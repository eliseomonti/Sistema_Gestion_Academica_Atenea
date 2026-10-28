package com.utn.atenea.comisiones.dto;

import com.utn.atenea.carreras.PeriodoLectivo;
import com.utn.atenea.comisiones.RolDocente;
import com.utn.atenea.comisiones.Turno;

import java.util.List;

/** Una materia de comisión que dicta el docente que consulta, con su rol en ella y los horarios. */
public record MateriaDocenteResponse(
        Long comisionMateriaId,
        Long comisionId,
        String nombreComision,
        Turno turno,
        Long materiaId,
        String nombreMateria,
        int anioLectivo,
        PeriodoLectivo periodoLectivo,
        RolDocente rol,
        List<HorarioResponse> horarios
) {
}
