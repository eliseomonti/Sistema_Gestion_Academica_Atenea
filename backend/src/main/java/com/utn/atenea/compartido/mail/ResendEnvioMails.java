package com.utn.atenea.compartido.mail;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ResendEnvioMails implements EnvioMails {

    private final Resend resend;
    private final String remitente;
    private final String redirigirA;

    ResendEnvioMails(@Value("${atenea.mail.resend-api-key}") String apiKey,
                     @Value("${atenea.mail.remitente}") String remitente,
                     @Value("${atenea.mail.redirigir-a}") String redirigirA) {
        this.resend = apiKey.isBlank() ? null : new Resend(apiKey);
        this.remitente = remitente;
        this.redirigirA = redirigirA;
    }

    @Override
    public boolean enviar(String destinatario, String asunto, String html) {
        String para = redirigirA.isBlank() ? destinatario : redirigirA;
        if (resend == null) {
            log.info("Mail no enviado (falta RESEND_API_KEY). Para: {} | Asunto: {}", para, asunto);
            return false;
        }
        CreateEmailOptions opciones = CreateEmailOptions.builder()
                .from(remitente)
                .to(para)
                .subject(asunto)
                .html(html)
                .build();
        try {
            resend.emails().send(opciones);
            return true;
        } catch (ResendException | RuntimeException e) {
            log.warn("No se pudo enviar el mail a {} ({}): {}", para, asunto, e.getMessage());
            return false;
        }
    }
}
