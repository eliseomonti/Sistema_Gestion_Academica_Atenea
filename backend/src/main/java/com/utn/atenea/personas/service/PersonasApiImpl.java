package com.utn.atenea.personas.service;

import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.EstudianteResumen;
import com.utn.atenea.personas.InscripcionCarreraResumen;
import com.utn.atenea.personas.PersonasApi;
import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.model.InscripcionCarrera;
import com.utn.atenea.personas.repository.InscripcionCarreraRepository;
import com.utn.atenea.personas.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonasApiImpl implements PersonasApi {

    private final PersonaRepository personaRepository;
    private final InscripcionCarreraRepository inscripcionCarreraRepository;

    @Override
    public List<DocenteResumen> buscarDocentes(Collection<Long> personaIds) {
        if (personaIds.isEmpty()) {
            return List.of();
        }
        return personaRepository.findByIdInAndLegajoDocenteIsNotNull(personaIds).stream()
                .map(persona -> new DocenteResumen(persona.getId(), persona.getNombre(), persona.getApellido(),
                        persona.getLegajoDocente(), persona.isActivo()))
                .toList();
    }

    @Override
    public List<InscripcionCarreraResumen> inscripcionesCarrera(Long estudianteId) {
        return inscripcionCarreraRepository.findByEstudianteId(estudianteId).stream()
                .map(PersonasApiImpl::toResumen)
                .toList();
    }

    @Override
    public List<InscripcionCarreraResumen> buscarInscripcionesCarrera(Collection<Long> inscripcionCarreraIds) {
        return inscripcionCarreraRepository.findAllById(inscripcionCarreraIds).stream()
                .map(PersonasApiImpl::toResumen)
                .toList();
    }

    @Override
    public List<EstudianteResumen> buscarEstudiantes(Collection<Long> personaIds) {
        if (personaIds.isEmpty()) {
            return List.of();
        }
        return personaRepository.findByIdInAndLegajoEstudianteIsNotNull(personaIds).stream()
                .map(persona -> new EstudianteResumen(persona.getId(), persona.getNombre(), persona.getApellido(),
                        persona.getLegajoEstudiante(), persona.getEmail()))
                .toList();
    }

    // getEstudiante().getId() no inicializa el proxy: el id ya está en la fila de inscripcion_carrera.
    private static InscripcionCarreraResumen toResumen(InscripcionCarrera inscripcion) {
        return new InscripcionCarreraResumen(inscripcion.getId(), inscripcion.getEstudiante().getId(),
                inscripcion.getPlanEstudioId(), inscripcion.getEstado() == EstadoInscripcionCarrera.ACTIVA);
    }
}
