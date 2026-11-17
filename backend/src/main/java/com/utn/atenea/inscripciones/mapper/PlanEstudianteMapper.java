package com.utn.atenea.inscripciones.mapper;

import com.utn.atenea.carreras.CorrelativasDeMateria;
import com.utn.atenea.inscripciones.dto.CorrelatividadesMateriaEstudianteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlanEstudianteMapper {

    CorrelatividadesMateriaEstudianteResponse toResponse(CorrelativasDeMateria correlativas);

    List<CorrelatividadesMateriaEstudianteResponse> toResponse(List<CorrelativasDeMateria> correlativas);
}
