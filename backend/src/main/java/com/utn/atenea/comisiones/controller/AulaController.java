package com.utn.atenea.comisiones.controller;

import com.utn.atenea.comisiones.dto.AulaResponse;
import com.utn.atenea.comisiones.service.AulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/aulas")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService aulaService;

    // Sólo las activas, ordenadas por nombre: es el desplegable del horario.
    @GetMapping
    public List<AulaResponse> listar() {
        return aulaService.listarActivas();
    }
}
