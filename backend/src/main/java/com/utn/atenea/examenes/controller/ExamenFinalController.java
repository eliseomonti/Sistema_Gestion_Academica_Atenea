package com.utn.atenea.examenes.controller;

import com.utn.atenea.examenes.dto.AltaExamenFinalRequest;
import com.utn.atenea.examenes.dto.ExamenFinalFiltroRequest;
import com.utn.atenea.examenes.dto.ExamenFinalResponse;
import com.utn.atenea.examenes.dto.ModificacionExamenFinalRequest;
import com.utn.atenea.examenes.service.ExamenFinalService;
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
@RequestMapping("/api/admin/examenes-finales")
@RequiredArgsConstructor
public class ExamenFinalController {

    private final ExamenFinalService examenFinalService;

    // Ejemplo: GET /api/admin/examenes-finales?periodoId=5
    // Sin paginar: un turno tiene una mesa por materia, a lo sumo unas pocas decenas.
    @GetMapping
    public List<ExamenFinalResponse> listar(@Valid ExamenFinalFiltroRequest filtro) {
        return examenFinalService.listar(filtro);
    }

    @GetMapping("/{examenFinalId}")
    public ExamenFinalResponse detalle(@PathVariable Long examenFinalId) {
        return examenFinalService.detalle(examenFinalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamenFinalResponse alta(@Valid @RequestBody AltaExamenFinalRequest request) {
        return examenFinalService.alta(request);
    }

    @PutMapping("/{examenFinalId}")
    public ExamenFinalResponse modificar(@PathVariable Long examenFinalId,
                                         @Valid @RequestBody ModificacionExamenFinalRequest request) {
        return examenFinalService.modificar(examenFinalId, request);
    }

    // Borrado real, para una mesa mal cargada. Con inscriptos devuelve 409.
    @DeleteMapping("/{examenFinalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long examenFinalId) {
        examenFinalService.eliminar(examenFinalId);
    }
}
