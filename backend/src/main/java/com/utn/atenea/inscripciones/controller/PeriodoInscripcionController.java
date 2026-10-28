package com.utn.atenea.inscripciones.controller;

import com.utn.atenea.inscripciones.dto.AltaPeriodoInscripcionRequest;
import com.utn.atenea.inscripciones.dto.ModificacionPeriodoInscripcionRequest;
import com.utn.atenea.inscripciones.dto.PeriodoInscripcionFiltroRequest;
import com.utn.atenea.inscripciones.dto.PeriodoInscripcionResponse;
import com.utn.atenea.inscripciones.service.PeriodoInscripcionService;
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
@RequestMapping("/api/admin/periodos-inscripcion")
@RequiredArgsConstructor
public class PeriodoInscripcionController {

    private final PeriodoInscripcionService periodoInscripcionService;

    // Ejemplo: GET /api/admin/periodos-inscripcion?carreraId=3&anioLectivo=2026&tipo=FINAL
    // Sin paginar: una carrera tiene unos pocos períodos por año.
    @GetMapping
    public List<PeriodoInscripcionResponse> listar(@Valid PeriodoInscripcionFiltroRequest filtro) {
        return periodoInscripcionService.listar(filtro);
    }

    @GetMapping("/{periodoId}")
    public PeriodoInscripcionResponse detalle(@PathVariable Long periodoId) {
        return periodoInscripcionService.detalle(periodoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodoInscripcionResponse alta(@Valid @RequestBody AltaPeriodoInscripcionRequest request) {
        return periodoInscripcionService.alta(request);
    }

    @PutMapping("/{periodoId}")
    public PeriodoInscripcionResponse modificar(@PathVariable Long periodoId,
                                                @Valid @RequestBody ModificacionPeriodoInscripcionRequest request) {
        return periodoInscripcionService.modificar(periodoId, request);
    }

    @PostMapping("/{periodoId}/habilitar")
    public PeriodoInscripcionResponse habilitar(@PathVariable Long periodoId) {
        return periodoInscripcionService.habilitar(periodoId);
    }

    @PostMapping("/{periodoId}/deshabilitar")
    public PeriodoInscripcionResponse deshabilitar(@PathVariable Long periodoId) {
        return periodoInscripcionService.deshabilitar(periodoId);
    }

    // Borrado real, para un período mal cargado. Con inscripciones o mesas devuelve 409: va deshabilitar.
    @DeleteMapping("/{periodoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long periodoId) {
        periodoInscripcionService.eliminar(periodoId);
    }
}
