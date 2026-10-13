package com.utn.atenea.seguridad.service;

import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario " + username));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(usuario.getRoles().stream().map(rol -> rol.getNombre().name()).toArray(String[]::new))
                .disabled(!usuario.isEnabled())
                .accountLocked(!usuario.isAccountNonLocked())
                .build();
    }
}
