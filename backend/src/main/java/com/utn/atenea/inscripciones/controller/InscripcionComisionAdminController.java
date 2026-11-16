package com.utn.atenea.inscripciones.controller;

import com.utn.atenea.inscripciones.dto.InscriptoComisionResponse;
import com.utn.atenea.inscripciones.service.InscripcionComisionAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inscripciones-comision")
@RequiredArgsConstructor
public class InscripcionComisionAdminController {

    private final InscripcionComisionAdminService inscripcionComisionAdminService;

    @GetMapping
    public List<InscriptoComisionResponse> listar(@RequestParam Long comisionMateriaId) {
        return inscripcionComisionAdminService.listar(comisionMateriaId);
    }

    @PostMapping("/{inscripcionId}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable Long inscripcionId) {
        inscripcionComisionAdminService.darDeBaja(inscripcionId);
    }

}
