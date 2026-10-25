package com.utn.atenea.comisiones.mapper;

import com.utn.atenea.comisiones.dto.AulaResponse;
import com.utn.atenea.comisiones.model.Aula;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AulaMapper {

    AulaResponse toResponse(Aula aula);

    List<AulaResponse> toResponse(List<Aula> aulas);
}
