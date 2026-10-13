package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.dto.PlanEstudioResponse;
import com.utn.atenea.carreras.service.PlanEstudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carreras/{carreraId}/planes")
@RequiredArgsConstructor
public class PlanEstudioController {

    private final PlanEstudioService planEstudioService;

    @GetMapping
    public List<PlanEstudioResponse> listar(@PathVariable Long carreraId) {
        return planEstudioService.listarPorCarrera(carreraId);
    }
}
