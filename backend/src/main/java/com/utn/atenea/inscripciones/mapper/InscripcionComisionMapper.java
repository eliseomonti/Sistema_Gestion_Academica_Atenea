package com.utn.atenea.inscripciones.mapper;

import com.utn.atenea.comisiones.ComisionMateriaResumen;
import com.utn.atenea.inscripciones.dto.InscripcionComisionResponse;
import com.utn.atenea.inscripciones.model.InscripcionComision;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InscripcionComisionMapper {

    // La comisión y el nombre de la materia no los guarda inscripciones: llegan de comisiones y carreras.
    @Mapping(target = "id", source = "inscripcion.id")
    @Mapping(target = "comisionMateriaId", source = "inscripcion.comisionMateriaId")
    @Mapping(target = "nombreComision", source = "comisionMateria.nombreComision")
    @Mapping(target = "materiaId", source = "comisionMateria.materiaId")
    @Mapping(target = "nombreMateria", source = "nombreMateria")
    @Mapping(target = "periodoLectivo", source = "comisionMateria.periodoLectivo")
    @Mapping(target = "anioLectivo", source = "comisionMateria.anioLectivo")
    @Mapping(target = "fechaInscripcion", source = "inscripcion.fechaInscripcion")
    @Mapping(target = "estado", source = "inscripcion.estado")
    InscripcionComisionResponse toResponse(InscripcionComision inscripcion, ComisionMateriaResumen comisionMateria,
                                           String nombreMateria);
}
