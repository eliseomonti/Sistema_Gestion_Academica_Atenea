package com.utn.atenea.inscripciones.controller;

import com.utn.atenea.inscripciones.dto.InscripcionFinalRequest;
import com.utn.atenea.inscripciones.dto.InscripcionFinalResponse;
import com.utn.atenea.inscripciones.dto.OfertaFinalResponse;
import com.utn.atenea.inscripciones.service.AltaInscripcionFinalService;
import com.utn.atenea.inscripciones.service.InscripcionFinalService;
import com.utn.atenea.inscripciones.service.OfertaFinalService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Autoservicio: el estudiante sale del token, nunca de la URL. */
@RestController
@RequestMapping("/api/estudiante/inscripciones-final")
@RequiredArgsConstructor
public class InscripcionFinalController {

    private final InscripcionFinalService inscripcionFinalService;
    private final AltaInscripcionFinalService altaInscripcionFinalService;
    private final OfertaFinalService ofertaFinalService;

    // Lo que el estudiante puede elegir ahora: mesas de los turnos abiertos de sus carreras.
    @GetMapping("/oferta")
    public List<OfertaFinalResponse> oferta(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ofertaFinalService.ofertar(principal.personaId());
    }

    // Las mesas a las que está anotado y que todavía no se tomaron.
    @GetMapping
    public List<InscripcionFinalResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return inscripcionFinalService.listar(principal.personaId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionFinalResponse inscribir(@AuthenticationPrincipal UsuarioPrincipal principal,
                                              @Valid @RequestBody InscripcionFinalRequest request) {
        return altaInscripcionFinalService.inscribir(principal.personaId(), request);
    }

    @PostMapping("/{inscripcionId}/baja")
    public InscripcionFinalResponse darDeBaja(@AuthenticationPrincipal UsuarioPrincipal principal,
                                              @PathVariable Long inscripcionId) {
        return inscripcionFinalService.darDeBaja(principal.personaId(), inscripcionId);
    }
}
