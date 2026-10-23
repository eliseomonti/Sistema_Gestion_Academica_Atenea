package com.utn.atenea.personas.service;

import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.personas.dto.AltaPersonaResponse;
import com.utn.atenea.personas.dto.DatosPersonalesRequest;
import com.utn.atenea.personas.dto.DocenteListadoResponse;
import com.utn.atenea.personas.dto.PersonaResponse;
import com.utn.atenea.personas.mapper.PersonaMapper;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.PersonaRepository;
import com.utn.atenea.seguridad.CredencialesIniciales;
import com.utn.atenea.seguridad.NombreRol;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.utn.atenea.personas.repository.PersonaSpecifications.conActivo;
import static com.utn.atenea.personas.repository.PersonaSpecifications.docenteCoincideCon;
import static com.utn.atenea.personas.repository.PersonaSpecifications.esDocente;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocenteService {

    private final PersonaService personaService;
    private final PersonaRepository personaRepository;
    private final PersonaMapper personaMapper;

    /**
     * Docentes activos (o dados de baja, con activo en false) que coinciden con el texto buscado,
     * o todos sin texto, por apellido y nombre.
     */
    public Page<DocenteListadoResponse> listar(String buscar, boolean activo, Pageable pageable) {
        Specification<Persona> filtro = esDocente().and(conActivo(activo));
        if (buscar != null && !buscar.isBlank()) {
            filtro = filtro.and(docenteCoincideCon(buscar));
        }
        return personaRepository.findAll(filtro, PersonaService.ordenAlfabetico(pageable))
                .map(personaMapper::toDocenteListado);
    }

    /** Alta de una persona nueva como docente: persona, legajo y usuario en una sola transacción. */
    @Transactional
    public AltaPersonaResponse alta(DatosPersonalesRequest datos) {
        Persona docente = personaService.crear(datos);
        personaService.asignarLegajoDocente(docente);
        CredencialesIniciales credenciales = personaService.crearUsuario(docente, NombreRol.ROLE_DOCENTE);

        return new AltaPersonaResponse(docente.getId(), docente.getLegajoDocente(),
                credenciales.username(), credenciales.passwordTemporal());
    }

    /** Una persona que ya existe (por ejemplo, un alumno) pasa a ser también docente. Conserva su usuario. */
    @Transactional
    public PersonaResponse asignarRol(Long personaId) {
        Persona persona = personaService.buscarActiva(personaId);
        if (persona.esDocente()) {
            throw new RecursoDuplicadoException("La persona " + personaId + " ya es docente");
        }

        personaService.asignarLegajoDocente(persona);
        personaService.agregarRol(persona, NombreRol.ROLE_DOCENTE);
        return personaMapper.toResponse(persona);
    }
}
