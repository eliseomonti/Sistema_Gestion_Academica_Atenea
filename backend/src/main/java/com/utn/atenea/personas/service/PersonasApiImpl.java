package com.utn.atenea.personas.service;

import com.utn.atenea.personas.DocenteResumen;
import com.utn.atenea.personas.PersonasApi;
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
}
