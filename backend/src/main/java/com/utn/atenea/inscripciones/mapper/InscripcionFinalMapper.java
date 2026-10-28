package com.utn.atenea.inscripciones.mapper;

import com.utn.atenea.examenes.ExamenFinalResumen;
import com.utn.atenea.inscripciones.dto.InscripcionFinalResponse;
import com.utn.atenea.inscripciones.model.InscripcionFinal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InscripcionFinalMapper {

    // La mesa, el nombre de la materia y el del aula no los guarda inscripciones: llegan de examenes,
    // carreras y comisiones.
    @Mapping(target = "id", source = "inscripcion.id")
    @Mapping(target = "examenFinalId", source = "inscripcion.examenFinalId")
    @Mapping(target = "materiaId", source = "mesa.materiaId")
    @Mapping(target = "nombreMateria", source = "nombreMateria")
    @Mapping(target = "fecha", source = "mesa.fecha")
    @Mapping(target = "horaInicio", source = "mesa.horaInicio")
    @Mapping(target = "horaFin", source = "mesa.horaFin")
    @Mapping(target = "nombreAula", source = "nombreAula")
    @Mapping(target = "fechaInscripcion", source = "inscripcion.fechaInscripcion")
    @Mapping(target = "estado", source = "inscripcion.estado")
    InscripcionFinalResponse toResponse(InscripcionFinal inscripcion, ExamenFinalResumen mesa, String nombreMateria,
                                        String nombreAula);
}
