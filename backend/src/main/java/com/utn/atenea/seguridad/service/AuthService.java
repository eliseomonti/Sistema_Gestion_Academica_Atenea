package com.utn.atenea.seguridad.service;

import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.seguridad.dto.CambioPasswordRequest;
import com.utn.atenea.seguridad.dto.LoginRequest;
import com.utn.atenea.seguridad.dto.RefreshRequest;
import com.utn.atenea.seguridad.dto.TokenResponse;
import com.utn.atenea.seguridad.model.RefreshToken;
import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.RefreshTokenRepository;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.utn.atenea.seguridad.service.TokensAleatorios.hash;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Duration duracionRefreshToken;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       @Value("${atenea.jwt.refresh-token-duration}") Duration duracionRefreshToken) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.duracionRefreshToken = duracionRefreshToken;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

        Usuario usuario = usuarioRepository.findByUsername(request.username()).orElseThrow();
        usuario.setUltimoAcceso(LocalDateTime.now());
        return emitirTokens(usuario);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken actual = refreshTokenRepository.findByToken(hash(request.refreshToken()))
                .filter(token -> token.esValido(LocalDateTime.now()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido o vencido"));

        Usuario usuario = actual.getUsuario();
        if (!usuario.isEnabled() || !usuario.isAccountNonLocked()) {
            throw new BadCredentialsException("La cuenta está deshabilitada o bloqueada");
        }

        actual.revocar();
        return emitirTokens(usuario);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(hash(request.refreshToken())).ifPresent(RefreshToken::revocar);
    }

    /**
     * El usuario logueado cambia su contraseña. Se cierran todas sus sesiones, por si alguien más
     * la conocía, y se devuelven tokens nuevos para que la sesión de este pedido siga abierta.
     */
    @Transactional
    public TokenResponse cambiarPassword(Long usuarioId, CambioPasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BadCredentialsException("El usuario ya no existe"));
        // El access token sirve hasta que vence: si mientras tanto lo deshabilitaron, no se le dan tokens nuevos.
        if (!usuario.isEnabled() || !usuario.isAccountNonLocked()) {
            throw new BadCredentialsException("La cuenta está deshabilitada o bloqueada");
        }
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        if (request.passwordNueva().equals(request.passwordActual())) {
            throw new ReglaNegocioException("La contraseña nueva tiene que ser distinta de la actual.");
        }

        establecerPassword(usuario, request.passwordNueva());
        return emitirTokens(usuario);
    }

    /**
     * El único lugar donde se fija una contraseña: también lo va a usar el restablecimiento por mail.
     * Las sesiones abiertas se cierran porque se abrieron con la contraseña anterior.
     */
    void establecerPassword(Usuario usuario, String passwordNueva) {
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuario.setFechaCambioPassword(LocalDateTime.now());
        refreshTokenRepository.revocarTodosDelUsuario(usuario.getId());
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        String refreshToken = TokensAleatorios.generar();
        LocalDateTime ahora = LocalDateTime.now();
        refreshTokenRepository.save(
                new RefreshToken(usuario, hash(refreshToken), ahora, ahora.plus(duracionRefreshToken)));

        return new TokenResponse(
                jwtService.generarAccessToken(usuario),
                refreshToken,
                jwtService.getDuracionAccessToken().toSeconds());
    }
}
