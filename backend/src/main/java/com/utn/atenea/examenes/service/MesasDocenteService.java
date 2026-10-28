package com.utn.atenea.examenes.service;

import com.utn.atenea.carreras.CarrerasApi;
import com.utn.atenea.carreras.MateriaResumen;
import com.utn.atenea.comisiones.AulaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.examenes.dto.MesaDocenteResponse;
import com.utn.atenea.examenes.model.ExamenFinal;
import com.utn.atenea.examenes.repository.ExamenFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Autoservicio del docente: las mesas en cuyo tribunal está. docenteId es el personaId del token. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MesasDocenteService {

    private final ExamenFinalRepository examenFinalRepository;
    private final CarrerasApi carrerasApi;
    private final ComisionesApi comisionesApi;

    /** Las de ese año (sin año, el actual), por fecha y hora. */
    public List<MesaDocenteResponse> listar(Long docenteId, Integer anio) {
        int delAnio = anio != null ? anio : LocalDate.now().getYear();
        List<ExamenFinal> mesas = examenFinalRepository.delTribunal(docenteId,
                LocalDate.of(delAnio, 1, 1), LocalDate.of(delAnio, 12, 31));
        Map<Long, String> materias = carrerasApi
                .buscarMaterias(mesas.stream().map(ExamenFinal::getMateriaId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(MateriaResumen::id, MateriaResumen::nombre));
        Map<Long, String> aulas = comisionesApi
                .buscarAulas(mesas.stream().map(ExamenFinal::getAulaId).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(AulaResumen::id, AulaResumen::nombre));

        return mesas.stream()
                .map(mesa -> new MesaDocenteResponse(mesa.getId(), mesa.getMateriaId(), materias.get(mesa.getMateriaId()),
                        mesa.getFecha(), mesa.getHoraInicio(), mesa.getHoraFin(),
                        mesa.getAulaId() != null ? aulas.get(mesa.getAulaId()) : null))
                .toList();
    }
}
