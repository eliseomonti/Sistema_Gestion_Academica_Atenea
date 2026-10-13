package com.utn.atenea.seguridad.config;

import com.utn.atenea.seguridad.UsuarioPrincipal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;

public class JwtUsuarioPrincipalConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String CLAIM_USUARIO_ID = "usuarioId";
    public static final String CLAIM_PERSONA_ID = "personaId";
    public static final String CLAIM_ROLES = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                claimLong(jwt, CLAIM_USUARIO_ID),
                claimLong(jwt, CLAIM_PERSONA_ID),
                jwt.getSubject());

        List<String> roles = jwt.getClaimAsStringList(CLAIM_ROLES);
        List<GrantedAuthority> authorities = roles == null
                ? List.of()
                : roles.stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();

        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, authorities);
    }

    private static Long claimLong(Jwt jwt, String claim) {
        if (!(jwt.getClaims().get(claim) instanceof Number valor)) {
            throw new InvalidBearerTokenException("Al token le falta el claim " + claim);
        }
        return valor.longValue();
    }
}
