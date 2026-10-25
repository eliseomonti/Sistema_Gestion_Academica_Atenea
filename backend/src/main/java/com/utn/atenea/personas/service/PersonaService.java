package com.utn.atenea.personas.service;

import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.personas.dto.DatosPersonalesRequest;
import com.utn.atenea.personas.dto.PersonaResponse;
import com.utn.atenea.personas.mapper.PersonaMapper;
import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.InscripcionCarreraRepository;
import com.utn.atenea.personas.repository.PersonaRepository;
import com.utn.atenea.seguridad.CredencialesIniciales;
import com.utn.atenea.seguridad.NombreRol;
import com.utn.atenea.seguridad.SeguridadApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

/**
 * Lo común a estudiantes y docentes. Las altas y las asignaciones de rol las arman EstudianteService y
 * DocenteService con estos métodos; los que llegan desde un controller son buscarPorDni, modificar
 * y darDeBaja.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private static final int EDAD_MINIMA = 16;

    // El id desempata a los homónimos: sin un orden total, una persona podría repetirse o
    // faltar al pasar de página.
    private static final Sort ORDEN_ALFABETICO = Sort.by("apellido", "nombre", "id");

    private final PersonaRepository personaRepository;
    private final InscripcionCarreraRepository inscripcionCarreraRepository;
    private final PersonaMapper personaMapper;
    private final SeguridadApi seguridadApi;

    @Transactional(readOnly = true)
    public PersonaResponse buscarPorDni(String dni) {
        return personaRepository.findByDni(dni)
                .map(personaMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una persona con DNI " + dni));
    }

    /** La persona a la que se le va a dar un rol nuevo: tiene que existir y no estar dada de baja. */
    public Persona buscarActiva(Long personaId) {
        Persona persona = buscar(personaId);
        if (!persona.isActivo()) {
            throw new ReglaNegocioException("La persona " + personaId + " está dada de baja");
        }
        return persona;
    }

    /** Guarda una persona nueva, todavía sin legajo ni usuario. */
    public Persona crear(DatosPersonalesRequest datos) {
        if (personaRepository.existsByDni(datos.dni())) {
            throw new RecursoDuplicadoException("Ya existe una persona con DNI " + datos.dni());
        }
        if (personaRepository.existsByEmail(datos.email())) {
            throw new RecursoDuplicadoException("Ya existe una persona con email " + datos.email());
        }
        validarEdad(datos.fechaNacimiento());

        return personaRepository.save(new Persona(datos.nombre(), datos.apellido(), datos.dni(),
                datos.telefono(), datos.fechaNacimiento(), datos.email()));
    }

    /**
     * Reemplaza los datos personales; sirve igual para estudiantes y docentes. Una persona dada de baja
     * también se puede corregir. Legajos e inscripciones no se tocan.
     */
    public PersonaResponse modificar(Long personaId, DatosPersonalesRequest datos) {
        Persona persona = buscar(personaId);
        if (personaRepository.existsByDniAndIdNot(datos.dni(), personaId)) {
            throw new RecursoDuplicadoException("Ya existe otra persona con DNI " + datos.dni());
        }
        if (personaRepository.existsByEmailAndIdNot(datos.email(), personaId)) {
            throw new RecursoDuplicadoException("Ya existe otra persona con email " + datos.email());
        }
        validarEdad(datos.fechaNacimiento());

        String dniAnterior = persona.getDni();
        personaMapper.actualizar(datos, persona);
        // El username es el DNI (ver crearUsuario): si el DNI cambió, el usuario lo sigue.
        seguridadApi.reemplazarUsername(personaId, dniAnterior, persona.getDni());
        return personaMapper.toResponse(persona);
    }

    /**
     * La persona deja la institución, con todos sus roles: queda inactiva, sus inscripciones activas
     * pasan a BAJA y su usuario ya no puede entrar. No se borra nada: legajos e historial se conservan.
     */
    public PersonaResponse darDeBaja(Long personaId) {
        Persona persona = buscar(personaId);
        if (!persona.isActivo()) {
            throw new ReglaNegocioException("La persona " + personaId + " ya está dada de baja");
        }
        // Si se diera de baja al único admin, nadie podría volver a entrar a administrar el sistema.
        if (persona.esAdmin()) {
            throw new ReglaNegocioException("No se puede dar de baja a un administrador");
        }

        LocalDate hoy = LocalDate.now();
        inscripcionCarreraRepository.findByEstudianteIdAndEstado(personaId, EstadoInscripcionCarrera.ACTIVA)
                .forEach(inscripcion -> inscripcion.darDeBaja(hoy));
        persona.darDeBaja(hoy);
        seguridadApi.deshabilitarUsuario(personaId);
        return personaMapper.toResponse(persona);
    }

    // MAX + 1: si dos altas simultáneas toman el mismo número, el UNIQUE de la base rechaza la segunda.
    public void asignarLegajoEstudiante(Persona persona) {
        persona.asignarLegajoEstudiante(personaRepository.ultimoLegajoEstudiante() + 1);
    }

    public void asignarLegajoDocente(Persona persona) {
        persona.asignarLegajoDocente(personaRepository.ultimoLegajoDocente() + 1);
    }

    // El username es el DNI: ya es único y la persona lo conoce.
    public CredencialesIniciales crearUsuario(Persona persona, NombreRol rol) {
        return seguridadApi.crearUsuario(persona.getId(), persona.getDni(), rol);
    }

    /** Para quien ya tiene usuario: suma el rol y conserva su contraseña. */
    public void agregarRol(Persona persona, NombreRol rol) {
        seguridadApi.agregarRol(persona.getId(), rol);
    }

    /** La página pedida, ordenada por apellido y nombre. El orden que venga en el request se ignora. */
    static Pageable ordenAlfabetico(Pageable pedido) {
        return PageRequest.of(pedido.getPageNumber(), pedido.getPageSize(), ORDEN_ALFABETICO);
    }

    private Persona buscar(Long personaId) {
        return personaRepository.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la persona con id " + personaId));
    }

    private void validarEdad(LocalDate fechaNacimiento) {
        if (Period.between(fechaNacimiento, LocalDate.now()).getYears() < EDAD_MINIMA) {
            throw new ReglaNegocioException("La persona tiene que tener al menos " + EDAD_MINIMA + " años");
        }
    }
}
