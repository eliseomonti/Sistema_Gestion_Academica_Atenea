package com.utn.atenea.comisiones;

/** Lo que otro módulo necesita saber de un aula para asignarla y mostrarla. */
public record AulaResumen(Long id, String nombre, boolean activa) {
}
