package com.utn.atenea.seguridad.service;

import com.utn.atenea.compartido.exception.ReglaNegocioException;
import com.utn.atenea.compartido.mail.EnvioMails;
import com.utn.atenea.seguridad.EmailsDePersonas;
import com.utn.atenea.seguridad.dto.RestablecimientoPasswordRequest;
import com.utn.atenea.seguridad.model.PasswordResetToken;
import com.utn.atenea.seguridad.model.Usuario;
import com.utn.atenea.seguridad.repository.PasswordResetTokenRepository;
import com.utn.atenea.seguridad.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.utn.atenea.seguridad.service.TokensAleatorios.hash;

/**
 * Quien se olvidó la contraseña pide un link por mail y con ese link elige una nueva. Se puede pedir
 * sin sesión (con el email) o desde Mi cuenta.
 */
@Service
@Transactional
public class RecuperacionPasswordService {

    private static final Duration VIGENCIA_LINK = Duration.ofMinutes(30);
    /** Tope de links por usuario y por hora, para que no se pueda llenar de mails una casilla. */
    private static final int MAXIMO_LINKS_POR_HORA = 3;
    private static final String LINK_INVALIDO = "El link no es válido o ya venció. Pedí uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailsDePersonas emailsDePersonas;
    private final EnvioMails envioMails;
    private final AuthService authService;
    private final String urlApp;

    public RecuperacionPasswordService(UsuarioRepository usuarioRepository,
                                       PasswordResetTokenRepository passwordResetTokenRepository,
                                       EmailsDePersonas emailsDePersonas,
                                       EnvioMails envioMails,
                                       AuthService authService,
                                       @Value("${atenea.mail.url-app}") String urlApp) {
        this.usuarioRepository = usuarioRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailsDePersonas = emailsDePersonas;
        this.envioMails = envioMails;
        this.authService = authService;
        this.urlApp = urlApp;
    }

    /**
     * Pedido sin sesión. Si el email es de una persona con usuario habilitado y no pasó el tope, le
     * manda el link. Si no, no hace nada y tampoco avisa: así no se puede averiguar qué emails hay.
     */
    public void solicitarPorEmail(String email, String rol) {
        emailsDePersonas.personaIdConEmail(email)
                .flatMap(usuarioRepository::findByPersonaId)
                .filter(Usuario::isEnabled)
                .filter(usuario -> !superoElTope(usuario))
                .ifPresent(usuario -> enviarLink(usuario, rol));
    }

    /** Pedido desde Mi cuenta: acá sí se avisa si no se mandó, porque el usuario ya es conocido. */
    public void solicitarParaUsuario(Long usuarioId, String rol) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("La cuenta está deshabilitada"));
        if (superoElTope(usuario)) {
            throw new ReglaNegocioException("Ya pediste " + MAXIMO_LINKS_POR_HORA
                    + " links en la última hora. Probá de nuevo más tarde.");
        }
        if (!enviarLink(usuario, rol)) {
            throw new ReglaNegocioException("No se pudo mandar el mail. Probá de nuevo más tarde.");
        }
    }

    public void restablecer(RestablecimientoPasswordRequest request) {
        LocalDateTime ahora = LocalDateTime.now();
        PasswordResetToken token = passwordResetTokenRepository.findByToken(hash(request.token()))
                .filter(t -> t.esValido(ahora))
                .orElseThrow(() -> new ReglaNegocioException(LINK_INVALIDO));
        Usuario usuario = token.getUsuario();
        if (!usuario.isEnabled()) {
            throw new ReglaNegocioException(LINK_INVALIDO);
        }

        authService.establecerPassword(usuario, request.passwordNueva());
        // Incluye al token de este pedido: cada link sirve una sola vez.
        invalidarPendientes(usuario, ahora);
    }

    private boolean superoElTope(Usuario usuario) {
        LocalDateTime haceUnaHora = LocalDateTime.now().minusHours(1);
        return passwordResetTokenRepository.countByUsuarioIdAndFechaCreacionAfter(usuario.getId(), haceUnaHora)
                >= MAXIMO_LINKS_POR_HORA;
    }

    /** Devuelve true si el mail salió. */
    private boolean enviarLink(Usuario usuario, String rol) {
        LocalDateTime ahora = LocalDateTime.now();
        // Sólo vale el último link pedido.
        invalidarPendientes(usuario, ahora);
        String token = TokensAleatorios.generar();
        passwordResetTokenRepository.saveAndFlush(
                new PasswordResetToken(usuario, hash(token), ahora, ahora.plus(VIGENCIA_LINK)));

        // El rol ya viene validado (admin, docente o estudiante): se puede poner en la URL tal cual.
        String link = urlApp + "/restablecer-password?token=" + token + (rol == null ? "" : "&rol=" + rol);
        String html = """
                <p>Hola:</p>
                <p>Recibimos un pedido para cambiar la contraseña de tu usuario de Atenea, <strong>%s</strong>.</p>
                <p><a href="%s">Elegí una contraseña nueva</a>. El link vence en %d minutos y sirve una sola vez.</p>
                <p>Si no lo pediste vos, ignorá este mail: tu contraseña no cambia.</p>
                """.formatted(HtmlUtils.htmlEscape(usuario.getUsername()), link, VIGENCIA_LINK.toMinutes());
        return envioMails.enviar(emailsDePersonas.emailDe(usuario.getPersonaId()),
                "Cambiar tu contraseña de Atenea", html);
    }

    private void invalidarPendientes(Usuario usuario, LocalDateTime ahora) {
        passwordResetTokenRepository.findByUsuarioIdAndFechaUsoIsNull(usuario.getId())
                .forEach(token -> token.marcarUsado(ahora));
    }
}
