package com.utn.atenea.inscripciones.mapper;

import com.utn.atenea.inscripciones.dto.PeriodoInscripcionResponse;
import com.utn.atenea.inscripciones.model.PeriodoInscripcion;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PeriodoInscripcionMapper {

    PeriodoInscripcionResponse toResponse(PeriodoInscripcion periodo);

    List<PeriodoInscripcionResponse> toResponse(List<PeriodoInscripcion> periodos);
}
