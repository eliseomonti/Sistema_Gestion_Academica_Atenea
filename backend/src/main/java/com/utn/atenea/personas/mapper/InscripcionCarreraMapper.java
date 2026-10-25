package com.utn.atenea.personas.mapper;

import com.utn.atenea.personas.dto.InscripcionCarreraResponse;
import com.utn.atenea.personas.model.InscripcionCarrera;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InscripcionCarreraMapper {

    @Mapping(target = "personaId", source = "estudiante.id")
    @Mapping(target = "legajoEstudiante", source = "estudiante.legajoEstudiante")
    InscripcionCarreraResponse toResponse(InscripcionCarrera inscripcion);
}
