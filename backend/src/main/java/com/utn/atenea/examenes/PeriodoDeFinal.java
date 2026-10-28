package com.utn.atenea.examenes;

import java.time.LocalDateTime;

/** Un turno de finales: la ventana [fechaInicio, fechaCierre) en que los estudiantes se inscriben a sus mesas. */
public record PeriodoDeFinal(
        Long id,
        Long carreraId,
        int anioLectivo,
        LocalDateTime fechaInicio,
        LocalDateTime fechaCierre
) {
}
