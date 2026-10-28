package com.utn.atenea.inscripciones.mapper;

import com.utn.atenea.comisiones.MateriaDictada;
import com.utn.atenea.inscripciones.dto.OfertaComisionResponse;
import com.utn.atenea.inscripciones.dto.OfertaDocenteResponse;
import com.utn.atenea.inscripciones.dto.OfertaHorarioResponse;
import com.utn.atenea.personas.DocenteResumen;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OfertaComisionMapper {

    // Lo que no sabe comisiones llega como contexto, ya buscado para toda la oferta: los docentes
    // en personas, el cupo ocupado y las inscripciones del estudiante en inscripciones.
    @Mapping(target = "docentes", expression = "java(toDocentes(materia.docentes(), docentes))")
    @Mapping(target = "cupoDisponible", expression = "java(cupoDisponible(materia, inscriptos))")
    @Mapping(target = "inscripto", expression = "java(inscriptoEn.contains(materia.comisionMateriaId()))")
    OfertaComisionResponse toResponse(MateriaDictada materia,
                                      @Context Map<Long, DocenteResumen> docentes,
                                      @Context Map<Long, Long> inscriptos,
                                      @Context Set<Long> inscriptoEn);

    OfertaHorarioResponse toResponse(MateriaDictada.Horario horario);

    // El orden (titular primero) ya viene de comisiones.
    default List<OfertaDocenteResponse> toDocentes(List<MateriaDictada.Docente> asignados,
                                                   Map<Long, DocenteResumen> docentes) {
        return asignados.stream()
                .map(asignado -> {
                    DocenteResumen docente = docentes.get(asignado.docenteId());
                    return new OfertaDocenteResponse(docente.apellido(), docente.nombre(), asignado.rol());
                })
                .toList();
    }

    // Nunca negativo: si el admin bajó el cupo por debajo de los ya inscriptos, queda en 0.
    default Integer cupoDisponible(MateriaDictada materia, Map<Long, Long> inscriptos) {
        if (materia.cupoMaximo() == null) {
            return null;
        }
        long ocupado = inscriptos.getOrDefault(materia.comisionMateriaId(), 0L);
        return (int) Math.max(0, materia.cupoMaximo() - ocupado);
    }
}
