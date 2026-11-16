package com.utn.atenea.personas.controller;

import com.utn.atenea.compartido.mail.EnvioMails;
import org.mockito.ArgumentCaptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/** La contraseña temporal ya no viaja en la respuesta del alta: los tests la sacan del último mail. */
final class CredencialesDelMail {

    private static final Pattern PASSWORD = Pattern.compile("Contraseña temporal: <strong>([^<]+)</strong>");

    private CredencialesDelMail() {
    }

    static String password(EnvioMails envioMails) {
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(envioMails, atLeastOnce()).enviar(anyString(), anyString(), html.capture());
        Matcher matcher = PASSWORD.matcher(html.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
