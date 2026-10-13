package com.utn.atenea.seguridad.service;

import com.utn.atenea.seguridad.model.NombreRol;
import com.utn.atenea.seguridad.model.Rol;
import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @InjectMocks
    UsuarioDetailsService service;

    @Test
    void mapeaCredencialesYRoles() {
        Usuario usuario = new Usuario(42L, "jperez", "hash", LocalDateTime.now());
        usuario.agregarRol(rol(NombreRol.ROLE_DOCENTE));
        usuario.agregarRol(rol(NombreRol.ROLE_ALUMNO));
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(usuario));

        UserDetails details = service.loadUserByUsername("jperez");

        assertThat(details.getUsername()).isEqualTo("jperez");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_DOCENTE", "ROLE_ALUMNO");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void usuarioDeshabilitadoYBloqueado() {
        Usuario usuario = new Usuario(42L, "jperez", "hash", LocalDateTime.now());
        usuario.setEnabled(false);
        usuario.setAccountNonLocked(false);
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(usuario));

        UserDetails details = service.loadUserByUsername("jperez");

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void usuarioInexistente() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // Rol no tiene constructor público ni setters (sus filas las carga el script de la base).
    private static Rol rol(NombreRol nombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(nombre);
        return rol;
    }
}
