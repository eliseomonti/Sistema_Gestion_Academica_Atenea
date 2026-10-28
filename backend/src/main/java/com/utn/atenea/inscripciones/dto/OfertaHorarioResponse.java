package com.utn.atenea.inscripciones.dto;

import com.utn.atenea.comisiones.DiaSemana;

import java.time.LocalTime;

/** aula viene en null si la cursada de esa franja es virtual. */
public record OfertaHorarioResponse(
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String aula
) {
}
