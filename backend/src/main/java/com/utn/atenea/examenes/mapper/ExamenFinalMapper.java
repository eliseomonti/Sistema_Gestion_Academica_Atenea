package com.utn.atenea.examenes.mapper;

import com.utn.atenea.comisiones.AulaResumen;
import com.utn.atenea.examenes.dto.DocenteTribunalResponse;
import com.utn.atenea.examenes.dto.ExamenFinalResponse;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.personas.DocenteResumen;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExamenFinalMapper {

    // Los nombres son de otros módulos y llegan como contexto, ya buscados para todas las mesas: las
    // materias en carreras, las aulas en comisiones y los docentes en personas.
    @Mapping(target = "nombreMateria", expression = "java(nombresMaterias.get(mesa.getMateriaId()))")
    @Mapping(target = "nombreAula", expression = "java(mesa.getAulaId() != null ? aulas.get(mesa.getAulaId()).nombre() : null)")
    @Mapping(target = "docentes", expression = "java(toDocentes(mesa.getDocenteIds(), docentes))")
    ExamenFinalResponse toResponse(ExamenFinal mesa,
                                   @Context Map<Long, String> nombresMaterias,
                                   @Context Map<Long, AulaResumen> aulas,
                                   @Context Map<Long, DocenteResumen> docentes);

    default List<DocenteTribunalResponse> toDocentes(Collection<Long> docenteIds, Map<Long, DocenteResumen> docentes) {
        return docenteIds.stream()
                .map(docentes::get)
                .sorted(Comparator.comparing(DocenteResumen::apellido).thenComparing(DocenteResumen::nombre))
                .map(docente -> new DocenteTribunalResponse(docente.personaId(), docente.apellido(), docente.nombre()))
                .toList();
    }
}
