package com.utn.atenea.seguridad;

/**
 * Puerto: el nombre de la persona del usuario, para ponerlo en el access token. Las personas son de
 * personas, que está arriba: lo implementa NombresDePersonasImpl, allá.
 */
public interface NombresDePersonas {

    NombrePersona nombreDe(Long personaId);
}
