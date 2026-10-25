package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.dto.PlanMateriaResponse;
import com.utn.atenea.carreras.service.PlanMateriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carreras/{carreraId}/planes/{planEstudioId}/materias")
@RequiredArgsConstructor
public class PlanMateriaController {

    private final PlanMateriaService planMateriaService;

    // Ejemplo: GET /api/admin/carreras/3/planes/3/materias?anioCursado=1. Sin anioCursado, todas las del plan.
    @GetMapping
    public List<PlanMateriaResponse> listar(@PathVariable Long carreraId,
                                            @PathVariable Long planEstudioId,
                                            @RequestParam(required = false) Integer anioCursado) {
        return planMateriaService.listar(carreraId, planEstudioId, anioCursado);
    }
}
