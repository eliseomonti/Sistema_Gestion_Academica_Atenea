package com.utn.atenea.examenes.service;

import com.utn.atenea.comisiones.ComisionesApi;
import com.utn.atenea.compartido.exception.RecursoNoEncontradoException;
import com.utn.atenea.examenes.model.ExamenFinal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Qué cursadas y qué mesas puede ver y calificar un docente. Que esté asignado a la materia de la
 * comisión lo decide comisiones; el tribunal de una mesa es de examenes. Sin permiso es 403: que la
 * materia o la mesa existan no es un secreto.
 */
@Component
@RequiredArgsConstructor
class AccesoDocente {

    private final ComisionesApi comisionesApi;

    void validarCursada(Long docenteId, Long comisionMateriaId) {
        if (comisionesApi.buscarComisionesMateria(List.of(comisionMateriaId)).isEmpty()) {
            throw new RecursoNoEncontradoException("No existe la materia de comisión con id " + comisionMateriaId);
        }
        if (!comisionesApi.esDocenteDe(docenteId, comisionMateriaId)) {
            throw new AccessDeniedException("El docente no está asignado a la materia de comisión " + comisionMateriaId);
        }
    }

    void validarMesa(Long docenteId, ExamenFinal mesa) {
        if (!mesa.getDocenteIds().contains(docenteId)) {
            throw new AccessDeniedException("El docente no es del tribunal de la mesa " + mesa.getId());
        }
    }
}
