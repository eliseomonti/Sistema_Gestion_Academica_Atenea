package com.utn.atenea.personas.service;

import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.PersonaRepository;
import com.utn.atenea.seguridad.EmailsDePersonas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Implementa el puerto que declara seguridad: así seguridad no depende de personas. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailsDePersonasImpl implements EmailsDePersonas {

    private final PersonaRepository personaRepository;

    @Override
    public String emailDe(Long personaId) {
        return personaRepository.findById(personaId).orElseThrow().getEmail();
    }

    @Override
    public Optional<Long> personaIdConEmail(String email) {
        return personaRepository.findByEmail(email).map(Persona::getId);
    }
}
