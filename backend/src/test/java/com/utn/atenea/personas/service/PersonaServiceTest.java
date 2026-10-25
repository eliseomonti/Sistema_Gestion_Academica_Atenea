package com.utn.atenea.personas.service;

import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.personas.dto.DatosPersonalesRequest;
import com.utn.atenea.personas.dto.PersonaResponse;
import com.utn.atenea.personas.mapper.PersonaMapper;
import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.model.InscripcionCarrera;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.personas.repository.InscripcionCarreraRepository;
import com.utn.atenea.personas.repository.PersonaRepository;
import com.utn.atenea.seguridad.NombreRol;
import com.utn.atenea.seguridad.SeguridadApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {

    private static final String DNI = "30111222";
    private static final String EMAIL = "juan@mail.com";

    @Mock
    PersonaRepository personaRepository;

    @Mock
    InscripcionCarreraRepository inscripcionCarreraRepository;

    @Mock
    SeguridadApi seguridadApi;

    // El mapper real que genera MapStruct: modificar lo usa para pisar los datos.
    @Spy
    PersonaMapper personaMapper = Mappers.getMapper(PersonaMapper.class);

    @InjectMocks
    PersonaService service;

    @Test
    void crearGuardaLaPersonaActivaYSinLegajos() {
        when(personaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Persona persona = service.crear(datos(LocalDate.of(2000, 5, 10)));

        assertThat(persona.getNombre()).isEqualTo("Juan");
        assertThat(persona.getDni()).isEqualTo(DNI);
        assertThat(persona.getEmail()).isEqualTo(EMAIL);
        assertThat(persona.isActivo()).isTrue();
        assertThat(persona.esEstudiante()).isFalse();
        assertThat(persona.esDocente()).isFalse();
    }

    @Test
    void dniRepetidoDevuelveRecursoDuplicado() {
        when(personaRepository.existsByDni(DNI)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(datos(LocalDate.of(2000, 5, 10))))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining(DNI);
        verify(personaRepository, never()).save(any());
    }

    @Test
    void emailRepetidoDevuelveRecursoDuplicado() {
        when(personaRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(datos(LocalDate.of(2000, 5, 10))))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining(EMAIL);
        verify(personaRepository, never()).save(any());
    }

    @Test
    void menorDe16DevuelveReglaNegocio() {
        LocalDate cumple16Maniana = LocalDate.now().minusYears(16).plusDays(1);

        assertThatThrownBy(() -> service.crear(datos(cumple16Maniana)))
                .isInstanceOf(ReglaNegocioException.class);
        verify(personaRepository, never()).save(any());
    }

    @Test
    void quienCumple16HoyPuedeDarseDeAlta() {
        when(personaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Persona persona = service.crear(datos(LocalDate.now().minusYears(16)));

        assertThat(persona.getFechaNacimiento()).isEqualTo(LocalDate.now().minusYears(16));
    }

    @Test
    void asignaElLegajoSiguienteAlUltimo() {
        Persona persona = persona();
        when(personaRepository.ultimoLegajoDocente()).thenReturn(41L);
        when(personaRepository.ultimoLegajoEstudiante()).thenReturn(0L);

        service.asignarLegajoDocente(persona);
        service.asignarLegajoEstudiante(persona);

        assertThat(persona.getLegajoDocente()).isEqualTo(42L);
        assertThat(persona.getLegajoEstudiante()).isEqualTo(1L);
    }

    @Test
    void elUsernameEsElDni() {
        Persona persona = persona();

        service.crearUsuario(persona, NombreRol.ROLE_DOCENTE);

        verify(seguridadApi).crearUsuario(persona.getId(), DNI, NombreRol.ROLE_DOCENTE);
    }

    // --- Modificación ---

    @Test
    void modificarPisaLosDatosConservaLosLegajosYElUsernameSigueAlDni() {
        Persona persona = persona();
        persona.asignarLegajoDocente(42L);
        when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));

        PersonaResponse respuesta = service.modificar(7L, new DatosPersonalesRequest(
                "Juan Carlos", "Pérez", "30111223", "2239876543", LocalDate.of(2000, 5, 11), "jc@mail.com"));

        assertThat(persona.getNombre()).isEqualTo("Juan Carlos");
        assertThat(persona.getDni()).isEqualTo("30111223");
        assertThat(persona.getTelefono()).isEqualTo("2239876543");
        assertThat(persona.getFechaNacimiento()).isEqualTo(LocalDate.of(2000, 5, 11));
        assertThat(persona.getEmail()).isEqualTo("jc@mail.com");
        assertThat(persona.getLegajoDocente()).isEqualTo(42L);
        assertThat(respuesta.dni()).isEqualTo("30111223");
        verify(seguridadApi).reemplazarUsername(7L, DNI, "30111223");
    }

    @Test
    void modificarConElDniDeOtraPersonaDevuelveRecursoDuplicado() {
        Persona persona = persona();
        when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));
        when(personaRepository.existsByDniAndIdNot("30111223", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.modificar(7L, datos("30111223", EMAIL, LocalDate.of(2000, 5, 10))))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("30111223");
        assertThat(persona.getDni()).isEqualTo(DNI);
        verify(seguridadApi, never()).reemplazarUsername(anyLong(), anyString(), anyString());
    }

    @Test
    void modificarConElEmailDeOtraPersonaDevuelveRecursoDuplicado() {
        Persona persona = persona();
        when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));
        when(personaRepository.existsByEmailAndIdNot("otro@mail.com", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.modificar(7L, datos(DNI, "otro@mail.com", LocalDate.of(2000, 5, 10))))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("otro@mail.com");
        assertThat(persona.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void modificarAUnMenorDe16DevuelveReglaNegocio() {
        Persona persona = persona();
        when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));

        assertThatThrownBy(() -> service.modificar(7L, datos(LocalDate.now().minusYears(15))))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(persona.getFechaNacimiento()).isEqualTo(LocalDate.of(2000, 5, 10));
    }

    @Test
    void modificarUnaPersonaInexistenteDevuelveRecursoNoEncontrado() {
        when(personaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(7L, datos(LocalDate.of(2000, 5, 10))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // --- Baja ---

    @Test
    void darDeBajaDesactivaLaPersonaSusInscripcionesActivasYSuUsuario() {
        Persona persona = persona();
        InscripcionCarrera activa = new InscripcionCarrera(persona, 10L, LocalDate.of(2024, 3, 1));
        when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));
        when(inscripcionCarreraRepository.findByEstudianteIdAndEstado(7L, EstadoInscripcionCarrera.ACTIVA))
                .thenReturn(List.of(activa));

        PersonaResponse respuesta = service.darDeBaja(7L);

        assertThat(persona.isActivo()).isFalse();
        assertThat(persona.getFechaBaja()).isEqualTo(LocalDate.now());
        assertThat(activa.getEstado()).isEqualTo(EstadoInscripcionCarrera.BAJA);
        assertThat(activa.getFechaEstado()).isEqualTo(LocalDate.now());
        assertThat(respuesta.activo()).isFalse();
        verify(seguridadApi).deshabilitarUsuario(7L);
    }

    @Test
    void darDeBajaAQuienYaEstaDadoDeBajaDevuelveReglaNegocio() {
        Persona persona = persona();
        persona.darDeBaja(LocalDate.of(2025, 1, 1));
        when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));

        assertThatThrownBy(() -> service.darDeBaja(7L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(persona.getFechaBaja()).isEqualTo(LocalDate.of(2025, 1, 1));
        verify(seguridadApi, never()).deshabilitarUsuario(anyLong());
    }

    @Test
    void unAdministradorNoSePuedeDarDeBaja() {
        Persona admin = persona();
        // legajoAdmin no tiene setter: los administradores se cargan con el script de la base.
        ReflectionTestUtils.setField(admin, "legajoAdmin", 1L);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.darDeBaja(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("administrador");
        assertThat(admin.isActivo()).isTrue();
        verify(seguridadApi, never()).deshabilitarUsuario(anyLong());
    }

    private static DatosPersonalesRequest datos(LocalDate fechaNacimiento) {
        return datos(DNI, EMAIL, fechaNacimiento);
    }

    private static DatosPersonalesRequest datos(String dni, String email, LocalDate fechaNacimiento) {
        return new DatosPersonalesRequest("Juan", "Pérez", dni, "2234567890", fechaNacimiento, email);
    }

    private static Persona persona() {
        return new Persona("Juan", "Pérez", DNI, "2234567890", LocalDate.of(2000, 5, 10), EMAIL);
    }
}
