package com.utn.atenea.seguridad.service;

import com.utn.atenea.compartido.exception.RecursoDuplicadoException;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.seguridad.CredencialesIniciales;
import com.utn.atenea.seguridad.NombreRol;
import com.utn.atenea.seguridad.SeguridadApi;
import com.utn.atenea.seguridad.model.Rol;
import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.RolRepository;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class SeguridadApiImpl implements SeguridadApi {

    // Sin caracteres que se confunden al dictarlos o copiarlos a mano: 0/O, 1/l/I.
    private static final String CARACTERES_PASSWORD = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LARGO_PASSWORD = 10;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    // Que la persona no tenga ya un usuario lo garantiza el UNIQUE de usuario.persona_id.
    @Override
    public CredencialesIniciales crearUsuario(Long personaId, String username, NombreRol rol) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new RecursoDuplicadoException("Ya existe el usuario " + username);
        }

        String passwordTemporal = generarPasswordTemporal();
        Usuario usuario = new Usuario(personaId, username, passwordEncoder.encode(passwordTemporal), LocalDateTime.now());
        usuario.agregarRol(buscarRol(rol));
        usuarioRepository.save(usuario);

        return new CredencialesIniciales(username, passwordTemporal);
    }

    @Override
    public void agregarRol(Long personaId, NombreRol rol) {
        Usuario usuario = usuarioRepository.findByPersonaId(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La persona " + personaId + " no tiene usuario"));
        usuario.agregarRol(buscarRol(rol));
    }

    // El refresh token apunta al usuario por id y los permisos salen de personaId y roles: cambiar
    // el username no corta ninguna sesión.
    @Override
    public void reemplazarUsername(Long personaId, String anterior, String nuevo) {
        if (anterior.equals(nuevo)) {
            return;
        }
        usuarioRepository.findByPersonaId(personaId)
                .filter(usuario -> usuario.getUsername().equals(anterior))
                .ifPresent(usuario -> {
                    if (usuarioRepository.existsByUsername(nuevo)) {
                        throw new RecursoDuplicadoException("Ya existe el usuario " + nuevo);
                    }
                    usuario.setUsername(nuevo);
                });
    }

    // El login y el refresh ya rechazan a los usuarios deshabilitados. El access token que esté en
    // uso sirve hasta que vence (atenea.jwt.access-token-duration).
    @Override
    public void deshabilitarUsuario(Long personaId) {
        usuarioRepository.findByPersonaId(personaId).ifPresent(usuario -> usuario.setEnabled(false));
    }

    // Los roles se cargan con el script de la base: si falta uno, es un error de instalación.
    private Rol buscarRol(NombreRol nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + nombre + " en la tabla rol"));
    }

    private String generarPasswordTemporal() {
        StringBuilder password = new StringBuilder(LARGO_PASSWORD);
        for (int i = 0; i < LARGO_PASSWORD; i++) {
            password.append(CARACTERES_PASSWORD.charAt(secureRandom.nextInt(CARACTERES_PASSWORD.length())));
        }
        return password.toString();
    }
}
