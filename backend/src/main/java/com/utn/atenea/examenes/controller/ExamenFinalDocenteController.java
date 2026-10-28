package com.utn.atenea.examenes.controller;

import com.utn.atenea.examenes.dto.CargaNotasRequest;
import com.utn.atenea.examenes.dto.MesaDocenteResponse;
import com.utn.atenea.examenes.dto.NotasExamenFinalResponse;
import com.utn.atenea.examenes.service.MesasDocenteService;
import com.utn.atenea.examenes.service.NotasExamenFinalService;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Autoservicio: el docente sale del token, nunca de la URL. */
@RestController
@RequestMapping("/api/docente/examenes-finales")
@RequiredArgsConstructor
public class ExamenFinalDocenteController {

    private final MesasDocenteService mesasDocenteService;
    private final NotasExamenFinalService notasExamenFinalService;

    // Ejemplo: GET /api/docente/examenes-finales?anio=2026 (sin anio, el actual)
    @GetMapping
    public List<MesaDocenteResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                            @RequestParam(required = false) Integer anio) {
        return mesasDocenteService.listar(principal.personaId(), anio);
    }

    @GetMapping("/{examenFinalId}/notas")
    public NotasExamenFinalResponse notas(@AuthenticationPrincipal UsuarioPrincipal principal,
                                          @PathVariable Long examenFinalId) {
        return notasExamenFinalService.planilla(principal.personaId(), examenFinalId);
    }

    // Carga o corrige las notas que vienen; devuelve la planilla completa.
    @PutMapping("/{examenFinalId}/notas")
    public NotasExamenFinalResponse cargarNotas(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                @PathVariable Long examenFinalId,
                                                @Valid @RequestBody CargaNotasRequest request) {
        return notasExamenFinalService.cargar(principal.personaId(), examenFinalId, request);
    }
}
