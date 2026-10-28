package com.utn.atenea.examenes.controller;

import com.utn.atenea.examenes.dto.ActaCursadaResponse;
import com.utn.atenea.examenes.dto.AltaEvaluacionRequest;
import com.utn.atenea.examenes.dto.CargaCierreRequest;
import com.utn.atenea.examenes.dto.EstudianteCursadaResponse;
import com.utn.atenea.examenes.dto.EvaluacionResponse;
import com.utn.atenea.examenes.service.CierreCursadaService;
import com.utn.atenea.examenes.service.EstudiantesCursadaService;
import com.utn.atenea.examenes.service.EvaluacionService;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Autoservicio: el docente sale del token, nunca de la URL. La lista de sus materias de comisión está en
 * comisiones (MateriaDocenteController); acá, lo de cada una.
 */
@RestController
@RequestMapping("/api/docente/comisiones-materia/{comisionMateriaId}")
@RequiredArgsConstructor
public class CursadaDocenteController {

    private final EstudiantesCursadaService estudiantesCursadaService;
    private final EvaluacionService evaluacionService;
    private final CierreCursadaService cierreCursadaService;

    @GetMapping("/estudiantes")
    public List<EstudianteCursadaResponse> estudiantes(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                       @PathVariable Long comisionMateriaId) {
        return estudiantesCursadaService.listar(principal.personaId(), comisionMateriaId);
    }

    @GetMapping("/evaluaciones")
    public List<EvaluacionResponse> evaluaciones(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                 @PathVariable Long comisionMateriaId) {
        return evaluacionService.listar(principal.personaId(), comisionMateriaId);
    }

    @PostMapping("/evaluaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluacionResponse altaEvaluacion(@AuthenticationPrincipal UsuarioPrincipal principal,
                                             @PathVariable Long comisionMateriaId,
                                             @Valid @RequestBody AltaEvaluacionRequest request) {
        return evaluacionService.alta(principal.personaId(), comisionMateriaId, request);
    }

    @GetMapping("/cierre")
    public ActaCursadaResponse acta(@AuthenticationPrincipal UsuarioPrincipal principal,
                                    @PathVariable Long comisionMateriaId) {
        return cierreCursadaService.acta(principal.personaId(), comisionMateriaId);
    }

    // Cierra o corrige las condiciones que vienen; devuelve el acta completa.
    @PutMapping("/cierre")
    public ActaCursadaResponse cerrar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                      @PathVariable Long comisionMateriaId,
                                      @Valid @RequestBody CargaCierreRequest request) {
        return cierreCursadaService.cerrar(principal.personaId(), comisionMateriaId, request);
    }
}
