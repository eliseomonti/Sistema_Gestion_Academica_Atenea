package com.utn.atenea.comisiones.controller;

import com.utn.atenea.comisiones.dto.AltaComisionMateriaRequest;
import com.utn.atenea.comisiones.dto.ComisionMateriaResponse;
import com.utn.atenea.comisiones.dto.ModificacionComisionMateriaRequest;
import com.utn.atenea.comisiones.service.ComisionMateriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Las materias se ven en el detalle de la comisión (GET /api/admin/comisiones/{id}): acá sólo se modifican.
@RestController
@RequestMapping("/api/admin/comisiones/{comisionId}/materias")
@RequiredArgsConstructor
public class ComisionMateriaController {

    private final ComisionMateriaService comisionMateriaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComisionMateriaResponse agregar(@PathVariable Long comisionId,
                                           @Valid @RequestBody AltaComisionMateriaRequest request) {
        return comisionMateriaService.agregar(comisionId, request);
    }

    @PutMapping("/{comisionMateriaId}")
    public ComisionMateriaResponse modificar(@PathVariable Long comisionId, @PathVariable Long comisionMateriaId,
                                             @Valid @RequestBody ModificacionComisionMateriaRequest request) {
        return comisionMateriaService.modificar(comisionId, comisionMateriaId, request);
    }

    // Borrado real, para una materia mal cargada. Con historia (inscripciones, notas) devuelve 409.
    @DeleteMapping("/{comisionMateriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable Long comisionId, @PathVariable Long comisionMateriaId) {
        comisionMateriaService.quitar(comisionId, comisionMateriaId);
    }
}
