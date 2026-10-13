package com.utn.atenea.seguridad.service;

import com.utn.atenea.seguridad.config.JwtUsuarioPrincipalConverter;
import com.utn.atenea.seguridad.model.Usuario;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Emite los access tokens. Los claims son los que lee {@link JwtUsuarioPrincipalConverter}. */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Getter
    private final Duration duracionAccessToken;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${atenea.jwt.access-token-duration}") Duration duracionAccessToken) {
        this.jwtEncoder = jwtEncoder;
        this.duracionAccessToken = duracionAccessToken;
    }

    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        List<String> roles = usuario.getRoles().stream().map(rol -> rol.getNombre().name()).toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracionAccessToken))
                .claim(JwtUsuarioPrincipalConverter.CLAIM_USUARIO_ID, usuario.getId())
                .claim(JwtUsuarioPrincipalConverter.CLAIM_PERSONA_ID, usuario.getPersonaId())
                .claim(JwtUsuarioPrincipalConverter.CLAIM_ROLES, roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
