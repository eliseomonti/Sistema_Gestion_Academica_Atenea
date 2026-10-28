package com.utn.atenea.examenes.controller;

import com.utn.atenea.examenes.dto.CargaNotasRequest;
import com.utn.atenea.examenes.dto.EvaluacionResponse;
import com.utn.atenea.examenes.dto.ModificacionEvaluacionRequest;
import com.utn.atenea.examenes.dto.NotasEvaluacionResponse;
import com.utn.atenea.examenes.service.EvaluacionService;
import com.utn.atenea.examenes.service.NotasEvaluacionService;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Autoservicio: el docente sale del token, nunca de la URL. */
@RestController
@RequestMapping("/api/docente/evaluaciones/{evaluacionId}")
@RequiredArgsConstructor
public class EvaluacionDocenteController {

    private final EvaluacionService evaluacionService;
    private final NotasEvaluacionService notasEvaluacionService;

    @PutMapping
    public EvaluacionResponse modificar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                        @PathVariable Long evaluacionId,
                                        @Valid @RequestBody ModificacionEvaluacionRequest request) {
        return evaluacionService.modificar(principal.personaId(), evaluacionId, request);
    }

    // Borrado real, para una evaluación mal cargada. Con notas o recuperatorios devuelve 409.
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long evaluacionId) {
        evaluacionService.eliminar(principal.personaId(), evaluacionId);
    }

    @GetMapping("/notas")
    public NotasEvaluacionResponse notas(@AuthenticationPrincipal UsuarioPrincipal principal,
                                         @PathVariable Long evaluacionId) {
        return notasEvaluacionService.planilla(principal.personaId(), evaluacionId);
    }

    // Carga o corrige las notas que vienen; devuelve la planilla completa.
    @PutMapping("/notas")
    public NotasEvaluacionResponse cargarNotas(@AuthenticationPrincipal UsuarioPrincipal principal,
                                               @PathVariable Long evaluacionId,
                                               @Valid @RequestBody CargaNotasRequest request) {
        return notasEvaluacionService.cargar(principal.personaId(), evaluacionId, request);
    }
}
