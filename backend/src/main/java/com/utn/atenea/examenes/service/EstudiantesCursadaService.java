package com.utn.atenea.examenes.service;

import com.utn.atenea.examenes.InscriptoACursada;
import com.utn.atenea.examenes.Inscriptos;
import com.utn.atenea.examenes.dto.EstudianteCursadaResponse;
import com.utn.atenea.personas.EstudianteResumen;
import com.utn.atenea.personas.PersonasApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Autoservicio del docente: los estudiantes de una materia que dicta. docenteId es el personaId del token. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstudiantesCursadaService {

    private final AccesoDocente accesoDocente;
    private final Inscriptos inscriptos;
    private final PersonasApi personasApi;

    /** Los anotados a la materia de la comisión que no se dieron de baja, por apellido y nombre. */
    public List<EstudianteCursadaResponse> listar(Long docenteId, Long comisionMateriaId) {
        accesoDocente.validarCursada(docenteId, comisionMateriaId);
        List<InscriptoACursada> anotados = inscriptos.inscriptosALaCursada(comisionMateriaId);
        Map<Long, EstudianteResumen> estudiantes = personasApi
                .buscarEstudiantes(anotados.stream().map(InscriptoACursada::estudianteId).toList()).stream()
                .collect(Collectors.toMap(EstudianteResumen::personaId, Function.identity()));

        return anotados.stream()
                .map(anotado -> {
                    EstudianteResumen estudiante = estudiantes.get(anotado.estudianteId());
                    return new EstudianteCursadaResponse(anotado.inscripcionComisionId(), estudiante.personaId(),
                            estudiante.legajoEstudiante(), estudiante.apellido(), estudiante.nombre(),
                            estudiante.email());
                })
                .sorted(Comparator.comparing(EstudianteCursadaResponse::apellido)
                        .thenComparing(EstudianteCursadaResponse::nombre))
                .toList();
    }
}
