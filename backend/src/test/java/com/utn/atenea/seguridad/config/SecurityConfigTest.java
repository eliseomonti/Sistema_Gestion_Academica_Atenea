package com.utn.atenea.seguridad.config;

import com.utn.atenea.seguridad.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica las reglas de acceso por prefijo. Un 404 significa que la seguridad dejó pasar el
 * request (no hay controller en esa ruta); 401 y 403 significan que lo frenó.
 */
@WebMvcTest(controllers = SecurityConfigTest.PrincipalController.class)
@Import({SecurityConfig.class, SecurityConfigTest.PrincipalController.class})
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    // @WebMvcTest no levanta services; el AuthenticationManager de SecurityConfig necesita uno.
    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void authEsPublico() throws Exception {
        mvc.perform(get("/api/auth/login")).andExpect(status().isNotFound());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/carreras")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/carreras").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolIncorrectoDevuelve403() throws Exception {
        mvc.perform(get("/api/admin/carreras").header("Authorization", bearer("ROLE_ALUMNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolCorrectoPasa() throws Exception {
        mvc.perform(get("/api/admin/carreras").header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rutaFueraDeLosPrefijosSeRechaza() throws Exception {
        mvc.perform(get("/api/carreras").header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elControllerRecibeElUsuarioPrincipal() throws Exception {
        mvc.perform(get("/api/alumno/yo").header("Authorization", bearer("ROLE_ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(content().string("7-42-jperez"));
    }

    private String bearer(String rol) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("jperez")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim(JwtUsuarioPrincipalConverter.CLAIM_USUARIO_ID, 7L)
                .claim(JwtUsuarioPrincipalConverter.CLAIM_PERSONA_ID, 42L)
                .claim(JwtUsuarioPrincipalConverter.CLAIM_ROLES, List.of(rol))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @RestController
    static class PrincipalController {

        @GetMapping("/api/alumno/yo")
        String yo(@AuthenticationPrincipal UsuarioPrincipal principal) {
            return principal.usuarioId() + "-" + principal.personaId() + "-" + principal.username();
        }
    }
}
