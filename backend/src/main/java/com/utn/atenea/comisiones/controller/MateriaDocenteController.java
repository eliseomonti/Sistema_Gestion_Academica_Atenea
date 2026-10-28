package com.utn.atenea.comisiones.controller;

import com.utn.atenea.comisiones.dto.MateriaDocenteResponse;
import com.utn.atenea.comisiones.service.MateriaDocenteService;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Autoservicio: el docente sale del token, nunca de la URL. */
@RestController
@RequestMapping("/api/docente/comisiones-materia")
@RequiredArgsConstructor
public class MateriaDocenteController {

    private final MateriaDocenteService materiaDocenteService;

    // Ejemplo: GET /api/docente/comisiones-materia?anioLectivo=2026 (sin anioLectivo, el año actual)
    @GetMapping
    public List<MateriaDocenteResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                               @RequestParam(required = false) Integer anioLectivo) {
        return materiaDocenteService.listar(principal.personaId(), anioLectivo);
    }
}
