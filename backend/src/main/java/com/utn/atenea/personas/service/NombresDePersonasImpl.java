package com.utn.atenea.personas.service;

import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.PersonaRepository;
import com.utn.atenea.seguridad.NombrePersona;
import com.utn.atenea.seguridad.NombresDePersonas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa el puerto que declara seguridad: así seguridad no depende de personas. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NombresDePersonasImpl implements NombresDePersonas {

    private final PersonaRepository personaRepository;

    @Override
    public NombrePersona nombreDe(Long personaId) {
        Persona persona = personaRepository.findById(personaId).orElseThrow();
        return new NombrePersona(persona.getNombre(), persona.getApellido());
    }
}
