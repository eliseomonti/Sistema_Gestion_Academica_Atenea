package com.utn.atenea.examenes.controller;

import com.utn.atenea.examenes.dto.CursadaEstudianteResponse;
import com.utn.atenea.examenes.dto.FinalEstudianteResponse;
import com.utn.atenea.examenes.service.NotasCursadaService;
import com.utn.atenea.examenes.service.NotasFinalService;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Autoservicio: el estudiante sale del token, nunca de la URL. */
@RestController
@RequestMapping("/api/estudiante/notas")
@RequiredArgsConstructor
public class NotasEstudianteController {

    private final NotasCursadaService notasCursadaService;
    private final NotasFinalService notasFinalService;

    // Parciales y demás evaluaciones de cada cursada, con el acta de cierre.
    @GetMapping("/cursadas")
    public List<CursadaEstudianteResponse> cursadas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return notasCursadaService.listar(principal.personaId());
    }

    @GetMapping("/finales")
    public List<FinalEstudianteResponse> finales(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return notasFinalService.listar(principal.personaId());
    }
}
