package com.utn.atenea.comisiones.mapper;

import com.utn.atenea.comisiones.dto.ComisionMateriaResponse;
import com.utn.atenea.comisiones.dto.DocenteAsignadoResponse;
import com.utn.atenea.comisiones.dto.HorarioResponse;
import com.utn.atenea.comisiones.model.ComisionDocente;
import com.utn.atenea.comisiones.model.ComisionHorario;
import com.utn.atenea.comisiones.model.ComisionMateria;
import com.utn.atenea.personas.DocenteResumen;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ComisionMateriaMapper {

    // El nombre de la materia y los de los docentes no se guardan en comisiones: llegan como
    // contexto, ya buscados en carreras (por materiaId) y en personas (por docenteId).
    @Mapping(target = "nombreMateria", expression = "java(nombresMaterias.get(comisionMateria.getMateriaId()))")
    @Mapping(target = "docentes", expression = "java(toDocentes(comisionMateria.getDocentes(), docentes))")
    @Mapping(target = "horarios", expression = "java(toHorarios(comisionMateria.getHorarios()))")
    ComisionMateriaResponse toResponse(ComisionMateria comisionMateria,
                                       @Context Map<Long, String> nombresMaterias,
                                       @Context Map<Long, DocenteResumen> docentes);

    @Mapping(target = "aulaId", source = "aula.id")
    @Mapping(target = "aulaNombre", source = "aula.nombre")
    HorarioResponse toResponse(ComisionHorario horario);

    /**
     * Por día y hora. Se ordena acá y no con @OrderBy porque @OrderBy sólo actúa al leer de la base:
     * una materia recién guardada tiene los horarios en el orden en que llegaron.
     */
    default List<HorarioResponse> toHorarios(List<ComisionHorario> horarios) {
        return horarios.stream()
                .sorted(Comparator.comparing(ComisionHorario::getDiaSemana)
                        .thenComparing(ComisionHorario::getHoraInicio))
                .map(this::toResponse)
                .toList();
    }

    /** El titular primero; después, por apellido. */
    default List<DocenteAsignadoResponse> toDocentes(Set<ComisionDocente> asignados,
                                                     Map<Long, DocenteResumen> docentes) {
        return asignados.stream()
                .map(asignado -> {
                    DocenteResumen docente = docentes.get(asignado.getDocenteId());
                    return new DocenteAsignadoResponse(asignado.getDocenteId(), docente.apellido(),
                            docente.nombre(), asignado.getRol());
                })
                .sorted(Comparator.comparing(DocenteAsignadoResponse::rol)
                        .thenComparing(DocenteAsignadoResponse::apellido))
                .toList();
    }
}
