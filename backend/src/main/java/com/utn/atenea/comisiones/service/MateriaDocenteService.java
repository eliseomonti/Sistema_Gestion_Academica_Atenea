package com.utn.atenea.comisiones.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaResumen;
import com.utn.atenea.comisiones.RolDocente;
import com.utn.atenea.comisiones.dto.MateriaDocenteResponse;
import com.utn.atenea.comisiones.mapper.ComisionMateriaMapper;
import com.utn.atenea.comisiones.model.ComisionDocente;
import com.utn.atenea.comisiones.model.ComisionMateria;
import com.utn.atenea.comisiones.repository.ComisionMateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Autoservicio del docente: las materias de comisión que dicta. docenteId es el personaId del token. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MateriaDocenteService {

    private final ComisionMateriaRepository comisionMateriaRepository;
    private final CarrerasApi carrerasApi;
    private final ComisionMateriaMapper comisionMateriaMapper;

    /** Las de comisiones activas de ese año lectivo (sin año, el actual), por período y comisión. */
    public List<MateriaDocenteResponse> listar(Long docenteId, Integer anioLectivo) {
        int anio = anioLectivo != null ? anioLectivo : LocalDate.now().getYear();
        List<ComisionMateria> asignadas = comisionMateriaRepository.asignadasAlDocente(docenteId, anio);
        Map<Long, String> nombresMaterias = carrerasApi
                .buscarMaterias(asignadas.stream().map(ComisionMateria::getMateriaId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(MateriaResumen::id, MateriaResumen::nombre));

        return asignadas.stream()
                .map(materia -> new MateriaDocenteResponse(materia.getId(), materia.getComision().getId(),
                        materia.getComision().getNombre(), materia.getComision().getTurno(), materia.getMateriaId(),
                        nombresMaterias.get(materia.getMateriaId()), materia.getComision().getAnioLectivo(),
                        materia.getPeriodoLectivo(), rolDe(materia, docenteId),
                        comisionMateriaMapper.toHorarios(materia.getHorarios())))
                .toList();
    }

    private static RolDocente rolDe(ComisionMateria materia, Long docenteId) {
        return materia.getDocentes().stream()
                .filter(asignado -> asignado.getDocenteId().equals(docenteId))
                .map(ComisionDocente::getRol)
                .findFirst()
                .orElseThrow();
    }
}
