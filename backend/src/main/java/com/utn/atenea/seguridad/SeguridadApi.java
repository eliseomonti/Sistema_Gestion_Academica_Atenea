package com.utn.atenea.seguridad;

public interface SeguridadApi {

    /**
     * Crea el usuario de una persona que todavía no tiene, con una contraseña temporal aleatoria.
     * La contraseña en claro sólo se devuelve acá: en la base queda el hash.
     */
    CredencialesIniciales crearUsuario(Long personaId, String username, NombreRol rol);

    /** Agrega un rol al usuario que la persona ya tiene, por ejemplo un alumno que pasa a ser docente. */
    void agregarRol(Long personaId, NombreRol rol);

    /**
     * Cambia el username de la persona de {@code anterior} a {@code nuevo}, sólo si hoy es {@code anterior}.
     * Si el usuario se llama de otra forma (el admin de la instalación es "admin") o no existe, no hace nada.
     */
    void reemplazarUsername(Long personaId, String anterior, String nuevo);

    /** El usuario de la persona deja de poder loguearse y de renovar su token. Si no tiene usuario, no hace nada. */
    void deshabilitarUsuario(Long personaId);
}
