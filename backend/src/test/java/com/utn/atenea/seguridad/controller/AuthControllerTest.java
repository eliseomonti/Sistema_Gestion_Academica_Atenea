package com.utn.atenea.seguridad.controller;

import com.jayway.jsonpath.JsonPath;
import com.utn.atenea.seguridad.NombreRol;
import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.RolRepository;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el login, refresh y logout contra la base real. {@code @Transactional} deshace todo
 * lo que se inserta al terminar cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String USERNAME = "test.auth";
    private static final String PASSWORD = "Secreta123";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired RolRepository rolRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;

    Usuario usuario;

    @BeforeEach
    void crearUsuario() {
        // persona es del módulo personas: se inserta con SQL para que este test no dependa de su código.
        jdbc.update("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email)
                VALUES ('Test', 'Auth', 'TEST-AUTH1', '000', '2000-01-01', 'test.auth@atenea.test')
                """);
        Long personaId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        usuario = new Usuario(personaId, USERNAME, passwordEncoder.encode(PASSWORD), LocalDateTime.now());
        usuario.agregarRol(rolRepository.findByNombre(NombreRol.ROLE_DOCENTE).orElseThrow());
        usuarioRepository.save(usuario);
    }

    @Test
    void loginCorrectoDevuelveTokensConLosClaims() throws Exception {
        String respuesta = body(login(USERNAME, PASSWORD).andExpect(status().isOk()));

        Jwt jwt = jwtDecoder.decode(JsonPath.read(respuesta, "$.accessToken"));
        assertThat(jwt.getSubject()).isEqualTo(USERNAME);
        assertThat(jwt.getClaimAsString("usuarioId")).isEqualTo(usuario.getId().toString());
        assertThat(jwt.getClaimAsString("personaId")).isEqualTo(usuario.getPersonaId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).isEqualTo(List.of("ROLE_DOCENTE"));
        assertThat(JsonPath.<String>read(respuesta, "$.refreshToken")).isNotBlank();
        assertThat(JsonPath.<Integer>read(respuesta, "$.expiresIn")).isEqualTo(15 * 60);
        assertThat(usuario.getUltimoAcceso()).isNotNull();
    }

    @Test
    void passwordIncorrectaDevuelve401() throws Exception {
        login(USERNAME, "otra")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void usuarioInexistenteDevuelve401() throws Exception {
        login("nadie", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDeshabilitadoDevuelve401() throws Exception {
        usuario.setEnabled(false);
        login(USERNAME, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void requestInvalidoDevuelve400() throws Exception {
        login("", "").andExpect(status().isBadRequest());
    }

    @Test
    void refreshRotaElToken() throws Exception {
        String refreshViejo = refreshToken(login(USERNAME, PASSWORD));

        String refreshNuevo = refreshToken(refresh(refreshViejo).andExpect(status().isOk()));
        assertThat(refreshNuevo).isNotEqualTo(refreshViejo);

        // El viejo ya no sirve; el nuevo sí.
        refresh(refreshViejo).andExpect(status().isUnauthorized());
        refresh(refreshNuevo).andExpect(status().isOk());
    }

    @Test
    void refreshInexistenteDevuelve401() throws Exception {
        refresh("token-inventado").andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevocaElRefreshToken() throws Exception {
        String refreshToken = refreshToken(login(USERNAME, PASSWORD));

        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private String body(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString();
    }

    private String refreshToken(ResultActions resultado) throws Exception {
        return JsonPath.read(body(resultado), "$.refreshToken");
    }
}
