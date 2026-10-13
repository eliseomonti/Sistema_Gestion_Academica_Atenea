package com.utn.atenea.carreras.dto;

import com.utn.atenea.carreras.model.ModalidadCarrera;

/** Fila del listado de carreras del admin: muestra el nombre y filtra por modalidad y activa. */
public record CarreraResponse(
        Long id,
        String nombre,
        ModalidadCarrera modalidad,
        boolean activa
) {
}
