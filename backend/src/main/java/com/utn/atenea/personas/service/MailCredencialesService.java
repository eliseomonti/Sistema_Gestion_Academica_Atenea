package com.utn.atenea.personas.service;

import com.utn.atenea.compartido.mail.EnvioMails;
import com.utn.atenea.personas.model.Persona;
import com.utn.atenea.seguridad.CredencialesIniciales;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** Arma y manda el mail con el usuario y la contraseña temporal de una persona recién dada de alta. */
@Service
public class MailCredencialesService {

    private final EnvioMails envioMails;
    private final String urlApp;

    public MailCredencialesService(EnvioMails envioMails, @Value("${atenea.mail.url-app}") String urlApp) {
        this.envioMails = envioMails;
        this.urlApp = urlApp;
    }

    /** Devuelve true si el mail salió. */
    public boolean enviar(Persona persona, CredencialesIniciales credenciales) {
        // Los datos los escribió el admin: se escapan para que no puedan meter HTML en el mail.
        String html = """
                <p>Hola %s:</p>
                <p>Ya tenés tu cuenta en Atenea.</p>
                <p>Usuario: <strong>%s</strong><br>
                Contraseña temporal: <strong>%s</strong></p>
                <p>Ingresá en <a href="%s">%s</a> y cambiá la contraseña.</p>
                """.formatted(
                HtmlUtils.htmlEscape(persona.getNombre()),
                HtmlUtils.htmlEscape(credenciales.username()),
                HtmlUtils.htmlEscape(credenciales.passwordTemporal()),
                urlApp, urlApp);
        return envioMails.enviar(persona.getEmail(), "Tu cuenta en Atenea", html);
    }
}
