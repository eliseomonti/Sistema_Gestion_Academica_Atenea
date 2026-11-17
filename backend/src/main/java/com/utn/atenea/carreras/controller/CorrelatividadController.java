package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.dto.CorrelatividadesMateriaResponse;
import com.utn.atenea.carreras.service.CorrelatividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carreras/{carreraId}/planes/{planEstudioId}/correlatividades")
@RequiredArgsConstructor
public class CorrelatividadController {

    private final CorrelatividadService correlatividadService;

    @GetMapping
    public List<CorrelatividadesMateriaResponse> listar(@PathVariable Long carreraId,
                                                        @PathVariable Long planEstudioId) {
        return correlatividadService.delPlan(carreraId, planEstudioId);
    }
}
