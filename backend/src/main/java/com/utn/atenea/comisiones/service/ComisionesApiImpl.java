package com.utn.atenea.comisiones.service;

import com.utn.atenea.comisiones.ComisionMateriaResumen;
import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.comisiones.MateriaDictada;
import com.utn.atenea.comisiones.model.ComisionDocente;
import com.utn.atenea.comisiones.model.ComisionHorario;
import com.utn.atenea.comisiones.model.ComisionMateria;
import com.utn.atenea.comisiones.repository.ComisionMateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComisionesApiImpl implements ComisionesApi {

    private final ComisionMateriaRepository comisionMateriaRepository;

    // MANDATORY: el bloqueo dura lo que la transacción, así que sin una de quien llama no serviría de nada.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ComisionMateriaResumen> bloquearComisionMateria(Long comisionMateriaId) {
        return comisionMateriaRepository.bloquear(comisionMateriaId).map(ComisionesApiImpl::toResumen);
    }

    @Override
    public List<ComisionMateriaResumen> buscarComisionesMateria(Collection<Long> comisionMateriaIds) {
        if (comisionMateriaIds.isEmpty()) {
            return List.of();
        }
        return comisionMateriaRepository.findByIdIn(comisionMateriaIds).stream()
                .map(ComisionesApiImpl::toResumen)
                .toList();
    }

    @Override
    public List<Long> idsDeComisionesMateria(Long materiaId, int anioLectivo) {
        return comisionMateriaRepository.idsPorMateriaYAnioLectivo(materiaId, anioLectivo);
    }

    @Override
    public List<MateriaDictada> materiasDictadas(Long planEstudioId, int anioLectivo) {
        return comisionMateriaRepository.dictadasEnElPlan(planEstudioId, anioLectivo).stream()
                .map(ComisionesApiImpl::toMateriaDictada)
                .toList();
    }

    private static MateriaDictada toMateriaDictada(ComisionMateria materia) {
        List<MateriaDictada.Horario> horarios = materia.getHorarios().stream()
                .sorted(Comparator.comparing(ComisionHorario::getDiaSemana).thenComparing(ComisionHorario::getHoraInicio))
                .map(horario -> new MateriaDictada.Horario(horario.getDiaSemana(), horario.getHoraInicio(),
                        horario.getHoraFin(), horario.getAula() != null ? horario.getAula().getNombre() : null))
                .toList();
        List<MateriaDictada.Docente> docentes = materia.getDocentes().stream()
                .sorted(Comparator.comparing(ComisionDocente::getRol))
                .map(docente -> new MateriaDictada.Docente(docente.getDocenteId(), docente.getRol()))
                .toList();
        return new MateriaDictada(materia.getId(), materia.getComision().getNombre(), materia.getComision().getTurno(),
                materia.getMateriaId(), materia.getPeriodoLectivo(), materia.getCupoMaximo(), horarios, docentes);
    }

    private static ComisionMateriaResumen toResumen(ComisionMateria materia) {
        return new ComisionMateriaResumen(materia.getId(), materia.getComision().getId(),
                materia.getComision().getNombre(), materia.getComision().getPlanEstudioId(),
                materia.getComision().getAnioLectivo(), materia.getMateriaId(), materia.getPeriodoLectivo(),
                materia.getCupoMaximo(), materia.getComision().isActiva());
    }
}
