package com.utn.atenea.carreras.mapper;

import com.utn.atenea.carreras.dto.PlanMateriaResponse;
import com.utn.atenea.carreras.model.PlanMateria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlanMateriaMapper {

    @Mapping(target = "materiaId", source = "materia.id")
    @Mapping(target = "nombre", source = "materia.nombre")
    PlanMateriaResponse toResponse(PlanMateria planMateria);

    List<PlanMateriaResponse> toResponse(List<PlanMateria> planMaterias);
}
