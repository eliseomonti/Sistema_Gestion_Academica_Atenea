package com.utn.atenea.carreras.mapper;

import com.utn.atenea.carreras.dto.PlanEstudioResponse;
import com.utn.atenea.carreras.model.PlanEstudio;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlanEstudioMapper {

    PlanEstudioResponse toResponse(PlanEstudio plan);

    List<PlanEstudioResponse> toResponse(List<PlanEstudio> planes);
}
