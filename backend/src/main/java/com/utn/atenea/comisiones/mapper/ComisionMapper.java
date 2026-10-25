package com.utn.atenea.comisiones.mapper;

import com.utn.atenea.comisiones.dto.ComisionDetalleResponse;
import com.utn.atenea.comisiones.dto.ComisionMateriaResponse;
import com.utn.atenea.comisiones.dto.ComisionResponse;
import com.utn.atenea.comisiones.model.Comision;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ComisionMapper {

    @Mapping(target = "cantidadMaterias", expression = "java(comision.getMaterias().size())")
    ComisionResponse toResponse(Comision comision);

    List<ComisionResponse> toResponse(List<Comision> comisiones);

    // Las materias llegan ya armadas: llevan nombres que comisiones no guarda (ver ComisionMateriaMapper).
    @Mapping(target = "materias", source = "materias")
    ComisionDetalleResponse toDetalleResponse(Comision comision, List<ComisionMateriaResponse> materias);
}
