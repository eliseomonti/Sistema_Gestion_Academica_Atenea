package com.utn.atenea.seguridad.controller;

import com.jayway.jsonpath.JsonPath;
import com.utn.atenea.compartido.mail.EnvioMails;
import com.utn.atenea.seguridad.NombreRol;
import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.RolRepository;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pedir el link por mail y elegir la contraseña nueva, contra la base real. El mail se reemplaza por un mock. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecuperacionPasswordTest {

    private static final String USERNAME = "test.recuperacion";
    private static final String EMAIL = "test.recuperacion@atenea.test";
    private static final Pattern TOKEN_EN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired RolRepository rolRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EnvioMails envioMails;

    @BeforeEach
    void crearUsuario() {
        jdbc.update("""
                INSERT INTO persona (nombre, apellido, dni, telefono, fecha_nacimiento, email)
                VALUES ('Test', 'Recuperacion', 'TEST-RECU1', '000', '2000-01-01', ?)
                """, EMAIL);
        Long personaId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Usuario usuario = new Usuario(personaId, USERNAME, passwordEncoder.encode("Anterior123"), LocalDateTime.now());
        usuario.agregarRol(rolRepository.findByNombre(NombreRol.ROLE_DOCENTE).orElseThrow());
        usuarioRepository.save(usuario);
        when(envioMails.enviar(any(), any(), any())).thenReturn(true);
    }

    @Test
    void conElLinkDelMailSeCambiaLaPasswordUnaSolaVez() throws Exception {
        recuperar(EMAIL).andExpect(status().isNoContent());
        String token = tokenDelMail();

        restablecer(token, "Nueva12345").andExpect(status().isNoContent());
        login("Nueva12345").andExpect(status().isOk());
        login("Anterior123").andExpect(status().isUnauthorized());

        restablecer(token, "OtraMas123").andExpect(status().isConflict());
    }

    @Test
    void pedirOtroLinkInvalidaElAnterior() throws Exception {
        recuperar(EMAIL);
        String primero = tokenDelMail();
        recuperar(EMAIL);

        restablecer(primero, "Nueva12345").andExpect(status().isConflict());
    }

    @Test
    void emailInexistenteDevuelve204SinMandarMail() throws Exception {
        recuperar("no.existe@atenea.test").andExpect(status().isNoContent());
        verifyNoInteractions(envioMails);
    }

    @Test
    void tokenInventadoDevuelve409() throws Exception {
        restablecer("inventado", "Nueva12345").andExpect(status().isConflict());
    }

    @Test
    void passwordCortaDevuelve400() throws Exception {
        recuperar(EMAIL);
        restablecer(tokenDelMail(), "corta").andExpect(status().isBadRequest());
    }

    @Test
    void elRolDeLaPantallaVaEnElLink() throws Exception {
        mvc.perform(post("/api/auth/recuperar-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"rol\":\"docente\"}"))
                .andExpect(status().isNoContent());

        assertThat(htmlDelMail()).contains("&rol=docente");
    }

    @Test
    void unRolDesconocidoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/recuperar-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"rol\":\"x\\\"><script>\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void despuesDelTopeNoMandaMasMailsPeroResponde204() throws Exception {
        for (int i = 0; i < 4; i++) {
            recuperar(EMAIL).andExpect(status().isNoContent());
        }
        verify(envioMails, times(3)).enviar(eq(EMAIL), anyString(), anyString());
    }

    @Test
    void desdeMiCuentaSePideElLinkYAvisaCuandoPasaElTope() throws Exception {
        String accessToken = JsonPath.read(login("Anterior123").andReturn().getResponse().getContentAsString(),
                "$.accessToken");

        for (int i = 0; i < 3; i++) {
            pedirLinkDesdeCuenta(accessToken).andExpect(status().isNoContent());
        }
        pedirLinkDesdeCuenta(accessToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya pediste 3 links en la última hora. Probá de nuevo más tarde."));
        assertThat(htmlDelMail()).contains("&rol=estudiante");
    }

    @Test
    void pedirElLinkDesdeMiCuentaSinSesionDevuelve401() throws Exception {
        mvc.perform(post("/api/cuenta/password/link").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    /** El HTML del último mail mandado. */
    private String htmlDelMail() {
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(envioMails, atLeastOnce()).enviar(eq(EMAIL), anyString(), html.capture());
        return html.getValue();
    }

    /** El token del último mail mandado. */
    private String tokenDelMail() {
        Matcher matcher = TOKEN_EN_LINK.matcher(htmlDelMail());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private ResultActions recuperar(String email) throws Exception {
        return mvc.perform(post("/api/auth/recuperar-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions pedirLinkDesdeCuenta(String accessToken) throws Exception {
        return mvc.perform(post("/api/cuenta/password/link").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"estudiante\"}"));
    }

    private ResultActions restablecer(String token, String passwordNueva) throws Exception {
        return mvc.perform(post("/api/auth/restablecer-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"passwordNueva\":\"" + passwordNueva + "\"}"));
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + password + "\"}"));
    }
}
