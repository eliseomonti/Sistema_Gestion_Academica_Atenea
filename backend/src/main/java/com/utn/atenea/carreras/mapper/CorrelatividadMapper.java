package com.utn.atenea.carreras.mapper;

import com.utn.atenea.carreras.CorrelativasDeMateria;
import com.utn.atenea.carreras.dto.CorrelatividadesMateriaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CorrelatividadMapper {

    CorrelatividadesMateriaResponse toResponse(CorrelativasDeMateria correlativas);

    List<CorrelatividadesMateriaResponse> toResponse(List<CorrelativasDeMateria> correlativas);
}
