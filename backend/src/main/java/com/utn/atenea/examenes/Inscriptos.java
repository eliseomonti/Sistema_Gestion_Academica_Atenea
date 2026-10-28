package com.utn.atenea.examenes;

import java.util.List;

/**
 * Puerto: lo que examenes necesita saber de las inscripciones a cursadas y a finales. Son de
 * inscripciones, que está arriba: lo implementa InscriptosImpl, allá.
 */
public interface Inscriptos {

    /** Las inscripciones a comisión del estudiante que no se dio de baja, de todas sus carreras. */
    List<InscriptoACursada> cursadasDelEstudiante(Long estudianteId);

    /** Las inscripciones a mesa de final del estudiante que no se dio de baja, de todas sus carreras. */
    List<InscriptoAFinal> finalesDelEstudiante(Long estudianteId);

    /** Los estudiantes anotados a la materia de la comisión que no se dieron de baja. */
    List<InscriptoACursada> inscriptosALaCursada(Long comisionMateriaId);

    /** Los estudiantes anotados a la mesa que no se dieron de baja. */
    List<InscriptoAFinal> inscriptosALaMesa(Long examenFinalId);
}
