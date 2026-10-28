package com.utn.atenea.inscripciones.controller;

import com.utn.atenea.inscripciones.dto.PlanEstudianteResponse;
import com.utn.atenea.inscripciones.service.PlanEstudianteService;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Autoservicio: el estudiante sale del token, nunca de la URL. */
@RestController
@RequestMapping("/api/estudiante/plan-estudio")
@RequiredArgsConstructor
public class PlanEstudianteController {

    private final PlanEstudianteService planEstudianteService;

    // Uno por cada carrera en la que está inscripto: casi siempre una lista de un elemento.
    @GetMapping
    public List<PlanEstudianteResponse> planes(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return planEstudianteService.planes(principal.personaId());
    }
}
