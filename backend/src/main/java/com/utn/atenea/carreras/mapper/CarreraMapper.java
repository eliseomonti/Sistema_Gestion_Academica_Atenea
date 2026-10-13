package com.utn.atenea.carreras.mapper;

import com.utn.atenea.carreras.dto.CarreraResponse;
import com.utn.atenea.carreras.model.Carrera;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CarreraMapper {

    CarreraResponse toResponse(Carrera carrera);

    List<CarreraResponse> toResponse(List<Carrera> carreras);
}
