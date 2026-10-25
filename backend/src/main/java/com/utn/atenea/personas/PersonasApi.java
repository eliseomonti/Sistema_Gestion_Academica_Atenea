package com.utn.atenea.personas;

import java.util.Collection;
import java.util.List;

public interface PersonasApi {

    /**
     * Los docentes con esos ids de persona, activos o dados de baja (activo lo distingue). Las
     * personas que no existen o no son docentes no vienen: quien llama compara contra los ids que pidió.
     */
    List<DocenteResumen> buscarDocentes(Collection<Long> personaIds);
}
