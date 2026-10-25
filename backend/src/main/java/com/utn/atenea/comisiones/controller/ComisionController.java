package com.utn.atenea.comisiones.controller;

import com.utn.atenea.comisiones.dto.AltaComisionRequest;
import com.utn.atenea.comisiones.dto.ComisionDetalleResponse;
import com.utn.atenea.comisiones.dto.ComisionFiltroRequest;
import com.utn.atenea.comisiones.dto.ComisionResponse;
import com.utn.atenea.comisiones.dto.ModificacionComisionRequest;
import com.utn.atenea.comisiones.service.ComisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comisiones")
@RequiredArgsConstructor
public class ComisionController {

    private final ComisionService comisionService;

    // Ejemplo: GET /api/admin/comisiones?carreraId=3&planEstudioId=3&anioLectivo=2026&anioCursado=1&turno=MANANA
    // Sin paginar: una carrera en un año tiene decenas de comisiones y la pantalla las muestra todas.
    @GetMapping
    public List<ComisionResponse> listar(@Valid ComisionFiltroRequest filtro) {
        return comisionService.listar(filtro);
    }

    @GetMapping("/{comisionId}")
    public ComisionDetalleResponse detalle(@PathVariable Long comisionId) {
        return comisionService.detalle(comisionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComisionResponse alta(@Valid @RequestBody AltaComisionRequest request) {
        return comisionService.alta(request);
    }

    @PutMapping("/{comisionId}")
    public ComisionResponse modificar(@PathVariable Long comisionId,
                                      @Valid @RequestBody ModificacionComisionRequest request) {
        return comisionService.modificar(comisionId, request);
    }

    // Borrado real, para una comisión mal cargada. Con historia (inscripciones, notas) devuelve 409: va la baja.
    @DeleteMapping("/{comisionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long comisionId) {
        comisionService.eliminar(comisionId);
    }

    // Baja lógica: la comisión se conserva con su historia, pero deja de usarse.
    @PostMapping("/{comisionId}/baja")
    public ComisionResponse darDeBaja(@PathVariable Long comisionId) {
        return comisionService.darDeBaja(comisionId);
    }
}
