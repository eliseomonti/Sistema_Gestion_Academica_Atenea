package com.utn.atenea.inscripciones.reglas;

/**
 * Estado de una materia para un estudiante. Están de más a menos avanzado: si la cursó varias veces,
 * vale el primero que alcanzó (una regular y una no regular dan REGULAR).
 */
public enum EstadoMateria {
    /** Final aprobado o cursada promocionada. */
    APROBADA,
    /** Cursada regular y todavía no vencida: puede rendir el final. */
    REGULAR,
    /** Inscripto a una comisión que todavía no tiene acta de cierre. */
    CURSANDO,
    REGULARIDAD_VENCIDA,
    NO_REGULAR,
    NO_CURSADA
}
