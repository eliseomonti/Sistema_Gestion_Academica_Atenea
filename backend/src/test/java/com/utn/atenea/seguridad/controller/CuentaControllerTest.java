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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el cambio de contraseña contra la base real. {@code @Transactional} deshace todo lo que
 * se inserta al terminar cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CuentaControllerTest {

    private static final String USERNAME = "test.cuenta";
    private static final String PASSWORD = "Secreta123";
    private static final String PASSWORD_NUEVA = "OtraSecreta456";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired RolRepository rolRepository;
    @Autowired PasswordEncoder passwordEncoder;

    Usuario usuario;

    @BeforeEach
    void crearUsuario() {
        // persona es del módulo personas: se inserta con SQL para que este test no dependa de su código.
        jdbc.update("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email)
                VALUES ('Test', 'Cuenta', 'TEST-CTA1', '000', '2000-01-01', 'test.cuenta@atenea.test')
                """);
        Long personaId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        usuario = new Usuario(personaId, USERNAME, passwordEncoder.encode(PASSWORD), LocalDateTime.now());
        usuario.agregarRol(rolRepository.findByNombre(NombreRol.ROLE_ESTUDIANTE).orElseThrow());
        usuarioRepository.save(usuario);
    }

    @Test
    void cambioCorrectoCierraLasSesionesYDevuelveTokensNuevos() throws Exception {
        String accessToken = JsonPath.read(body(login(USERNAME, PASSWORD)), "$.accessToken");

        String respuesta = body(cambiar(accessToken, PASSWORD, PASSWORD_NUEVA).andExpect(status().isOk()));
        String refreshNuevo = JsonPath.read(respuesta, "$.refreshToken");

        login(USERNAME, PASSWORD).andExpect(status().isUnauthorized());
        login(USERNAME, PASSWORD_NUEVA).andExpect(status().isOk());
        assertThat(usuario.getFechaCambioPassword()).isNotNull();

        // Se consulta la base y no la entidad: el UPDATE masivo no actualiza las que ya están en memoria.
        Integer abiertas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE usuario_id = ? AND revocado = false",
                Integer.class, usuario.getId());
        assertThat(abiertas).isEqualTo(2); // la que devolvió el cambio y la del último login
        refresh(refreshNuevo).andExpect(status().isOk());
    }

    @Test
    void passwordActualIncorrectaDevuelve409() throws Exception {
        String accessToken = JsonPath.read(body(login(USERNAME, PASSWORD)), "$.accessToken");

        cambiar(accessToken, "otra", PASSWORD_NUEVA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La contraseña actual no es correcta."));
    }

    @Test
    void passwordNuevaIgualALaActualDevuelve409() throws Exception {
        String accessToken = JsonPath.read(body(login(USERNAME, PASSWORD)), "$.accessToken");

        cambiar(accessToken, PASSWORD, PASSWORD).andExpect(status().isConflict());
    }

    @Test
    void passwordNuevaCortaDevuelve400() throws Exception {
        String accessToken = JsonPath.read(body(login(USERNAME, PASSWORD)), "$.accessToken");

        cambiar(accessToken, PASSWORD, "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("passwordNueva"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(put("/api/cuenta/password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(PASSWORD, PASSWORD_NUEVA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDeshabilitadoConTokenVigenteDevuelve401() throws Exception {
        String accessToken = JsonPath.read(body(login(USERNAME, PASSWORD)), "$.accessToken");
        usuario.setEnabled(false);

        cambiar(accessToken, PASSWORD, PASSWORD_NUEVA).andExpect(status().isUnauthorized());
    }

    private ResultActions cambiar(String accessToken, String actual, String nueva) throws Exception {
        return mvc.perform(put("/api/cuenta/password").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(json(actual, nueva)));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private static String json(String actual, String nueva) {
        return "{\"passwordActual\":\"" + actual + "\",\"passwordNueva\":\"" + nueva + "\"}";
    }

    private String body(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString();
    }
}
