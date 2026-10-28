package com.utn.atenea.inscripciones.controller;

import com.utn.atenea.inscripciones.dto.InscripcionComisionRequest;
import com.utn.atenea.inscripciones.dto.InscripcionComisionResponse;
import com.utn.atenea.inscripciones.dto.OfertaMateriaResponse;
import com.utn.atenea.inscripciones.service.InscripcionComisionService;
import com.utn.atenea.inscripciones.service.OfertaComisionService;
import com.utn.atenea.seguridad.UsuarioPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Autoservicio: el estudiante sale del token, nunca de la URL. */
@RestController
@RequestMapping("/api/estudiante/inscripciones-comision")
@RequiredArgsConstructor
public class InscripcionComisionController {

    private final InscripcionComisionService inscripcionComisionService;
    private final OfertaComisionService ofertaComisionService;

    // Lo que el estudiante puede elegir ahora: materias de sus planes con período de cursada abierto.
    @GetMapping("/oferta")
    public List<OfertaMateriaResponse> oferta(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ofertaComisionService.ofertar(principal.personaId());
    }

    // Ejemplo: GET /api/estudiante/inscripciones-comision?anioLectivo=2026 (sin anioLectivo, el año actual)
    @GetMapping
    public List<InscripcionComisionResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                    @RequestParam(required = false) Integer anioLectivo) {
        return inscripcionComisionService.listar(principal.personaId(), anioLectivo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionComisionResponse inscribir(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                 @Valid @RequestBody InscripcionComisionRequest request) {
        return inscripcionComisionService.inscribir(principal.personaId(), request);
    }

    @PostMapping("/{inscripcionId}/baja")
    public InscripcionComisionResponse darDeBaja(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                 @PathVariable Long inscripcionId) {
        return inscripcionComisionService.darDeBaja(principal.personaId(), inscripcionId);
    }
}
