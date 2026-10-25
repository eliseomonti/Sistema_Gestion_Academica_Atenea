package com.utn.atenea.personas.mapper;

import com.utn.atenea.personas.dto.EstudianteListadoResponse;
import com.utn.atenea.personas.dto.DatosPersonalesRequest;
import com.utn.atenea.personas.dto.DocenteListadoResponse;
import com.utn.atenea.personas.dto.PersonaResponse;
import com.utn.atenea.personas.model.Persona;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PersonaMapper {

    PersonaResponse toResponse(Persona persona);

    @Mapping(target = "personaId", source = "id")
    @Mapping(target = "legajo", source = "legajoEstudiante")
    EstudianteListadoResponse toEstudianteListado(Persona persona);

    @Mapping(target = "personaId", source = "id")
    @Mapping(target = "legajo", source = "legajoDocente")
    DocenteListadoResponse toDocenteListado(Persona persona);

    // Pisa los datos personales. Legajos, activo y fecha de baja no tienen setter: MapStruct no los toca.
    void actualizar(DatosPersonalesRequest datos, @MappingTarget Persona persona);
}
