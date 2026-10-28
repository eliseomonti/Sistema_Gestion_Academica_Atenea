package com.utn.atenea.comisiones.dto;

import com.utn.atenea.comisiones.DiaSemana;

import java.time.LocalTime;

/** aulaId y aulaNombre vienen en null si la cursada es virtual. */
public record HorarioResponse(
        Long id,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long aulaId,
        String aulaNombre
) {
}
