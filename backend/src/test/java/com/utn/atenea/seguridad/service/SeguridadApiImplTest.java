package com.utn.atenea.seguridad.service;

import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.seguridad.CredencialesIniciales;
import com.utn.atenea.seguridad.NombreRol;
import com.utn.atenea.seguridad.model.Rol;
import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.RolRepository;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguridadApiImplTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    RolRepository rolRepository;

    // Encoder real: así se puede comprobar que lo guardado es un hash de la contraseña devuelta.
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    SeguridadApiImpl service;

    @BeforeEach
    void crearService() {
        service = new SeguridadApiImpl(usuarioRepository, rolRepository, passwordEncoder);
    }

    // Rol no tiene constructor público (sus filas las carga el script de la base): se usan mocks.
    @Test
    void crearUsuarioGuardaElHashYDevuelveLaPasswordTemporal() {
        Rol docente = mock(Rol.class);
        when(usuarioRepository.existsByUsername("30111222")).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ROLE_DOCENTE)).thenReturn(Optional.of(docente));

        CredencialesIniciales credenciales = service.crearUsuario(7L, "30111222", NombreRol.ROLE_DOCENTE);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        Usuario usuario = guardado.getValue();

        assertThat(credenciales.username()).isEqualTo("30111222");
        assertThat(credenciales.passwordTemporal()).matches("[A-HJ-NP-Za-km-z2-9]{10}");
        assertThat(usuario.getPersonaId()).isEqualTo(7L);
        assertThat(usuario.getUsername()).isEqualTo("30111222");
        assertThat(usuario.getPassword()).isNotEqualTo(credenciales.passwordTemporal());
        assertThat(passwordEncoder.matches(credenciales.passwordTemporal(), usuario.getPassword())).isTrue();
        assertThat(usuario.getRoles()).containsExactly(docente);
    }

    @Test
    void usernameRepetidoDevuelveRecursoDuplicado() {
        when(usuarioRepository.existsByUsername("30111222")).thenReturn(true);

        assertThatThrownBy(() -> service.crearUsuario(7L, "30111222", NombreRol.ROLE_DOCENTE))
                .isInstanceOf(RecursoDuplicadoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void agregarRolSumaElRolAlUsuarioExistente() {
        Rol estudiante = mock(Rol.class);
        Rol docente = mock(Rol.class);
        Usuario usuario = new Usuario(7L, "30111222", "hash", LocalDateTime.now());
        usuario.agregarRol(estudiante);
        when(usuarioRepository.findByPersonaId(7L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre(NombreRol.ROLE_DOCENTE)).thenReturn(Optional.of(docente));

        service.agregarRol(7L, NombreRol.ROLE_DOCENTE);

        assertThat(usuario.getRoles()).containsExactlyInAnyOrder(estudiante, docente);
    }

    @Test
    void agregarRolSinUsuarioDevuelveRecursoNoEncontrado() {
        when(usuarioRepository.findByPersonaId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregarRol(7L, NombreRol.ROLE_DOCENTE))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void reemplazarUsernameCambiaElQueEraElAnterior() {
        Usuario usuario = new Usuario(7L, "30111222", "hash", LocalDateTime.now());
        when(usuarioRepository.findByPersonaId(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("30111223")).thenReturn(false);

        service.reemplazarUsername(7L, "30111222", "30111223");

        assertThat(usuario.getUsername()).isEqualTo("30111223");
    }

    @Test
    void reemplazarUsernameNoTocaAUnUsuarioQueSeLlamaDistinto() {
        Usuario admin = new Usuario(1L, "admin", "hash", LocalDateTime.now());
        when(usuarioRepository.findByPersonaId(1L)).thenReturn(Optional.of(admin));

        service.reemplazarUsername(1L, "00000000", "30111222");

        assertThat(admin.getUsername()).isEqualTo("admin");
    }

    @Test
    void reemplazarUsernamePorUnoQueYaExisteDevuelveRecursoDuplicado() {
        Usuario usuario = new Usuario(7L, "30111222", "hash", LocalDateTime.now());
        when(usuarioRepository.findByPersonaId(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("30111223")).thenReturn(true);

        assertThatThrownBy(() -> service.reemplazarUsername(7L, "30111222", "30111223"))
                .isInstanceOf(RecursoDuplicadoException.class);
        assertThat(usuario.getUsername()).isEqualTo("30111222");
    }

    // Sin este corte, el existsByUsername encontraría al propio usuario y devolvería un 409.
    @Test
    void reemplazarUsernamePorElMismoNoHaceNada() {
        service.reemplazarUsername(7L, "30111222", "30111222");

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deshabilitarUsuarioLoDejaSinAcceso() {
        Usuario usuario = new Usuario(7L, "30111222", "hash", LocalDateTime.now());
        when(usuarioRepository.findByPersonaId(7L)).thenReturn(Optional.of(usuario));

        service.deshabilitarUsuario(7L);

        assertThat(usuario.isEnabled()).isFalse();
    }

    @Test
    void deshabilitarAUnaPersonaSinUsuarioNoHaceNada() {
        when(usuarioRepository.findByPersonaId(7L)).thenReturn(Optional.empty());

        assertThatCode(() -> service.deshabilitarUsuario(7L)).doesNotThrowAnyException();
    }
}
