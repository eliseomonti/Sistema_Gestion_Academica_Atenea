package com.utn.atenea.compartido.mail;

/**
 * Puerto para mandar mails. Quien lo usa no sabe qué proveedor hay detrás.
 * Si el envío falla, no lanza excepción: un mail que no sale no debe romper la operación que lo pidió.
 */
public interface EnvioMails {

    /** Devuelve true si el proveedor aceptó el mail. */
    boolean enviar(String destinatario, String asunto, String html);
}
