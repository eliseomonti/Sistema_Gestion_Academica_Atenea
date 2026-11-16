package com.utn.atenea.seguridad.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens opacos (refresh y restablecimiento de contraseña). En la base se guarda sólo el hash: si
 * alguien lee la tabla, no puede usarlos.
 */
final class TokensAleatorios {

    private static final int BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokensAleatorios() {
    }

    /** Base64 URL: se puede poner en un link sin escaparlo. */
    static String generar() {
        byte[] bytes = new byte[BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
