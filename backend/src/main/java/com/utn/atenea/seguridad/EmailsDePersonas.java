package com.utn.atenea.seguridad;

import java.util.Optional;

/**
 * Puerto: el email de las personas, para el restablecimiento de contraseña por mail. Las personas son
 * de personas, que está arriba: lo implementa EmailsDePersonasImpl, allá.
 */
public interface EmailsDePersonas {

    String emailDe(Long personaId);

    /** La persona que tiene ese email, si hay alguna. */
    Optional<Long> personaIdConEmail(String email);
}
