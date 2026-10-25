package com.utn.atenea.personas.controller;

import com.utn.atenea.personas.dto.AltaEstudianteRequest;
import com.utn.atenea.personas.dto.EstudianteListadoResponse;
import com.utn.atenea.personas.dto.AltaPersonaResponse;
import com.utn.atenea.personas.dto.InscripcionCarreraRequest;
import com.utn.atenea.personas.dto.InscripcionCarreraResponse;
import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.service.EstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    // Ejemplo: GET /api/admin/estudiantes?carreraId=3&buscar=gomez&page=0&size=20
    // PagedModel le da a la respuesta una forma fija: {"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}.
    @GetMapping
    public PagedModel<EstudianteListadoResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long carreraId,
            @RequestParam(defaultValue = "ACTIVA") EstadoInscripcionCarrera estado,
            @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(estudianteService.listar(buscar, carreraId, estado, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AltaPersonaResponse alta(@Valid @RequestBody AltaEstudianteRequest request) {
        return estudianteService.alta(request);
    }

    // Sirve para el estudiante que suma otra carrera y para la persona que todavía no es estudiante.
    @PostMapping("/{personaId}/inscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionCarreraResponse inscribir(@PathVariable Long personaId,
                                                @Valid @RequestBody InscripcionCarreraRequest request) {
        return estudianteService.inscribir(personaId, request);
    }

    // La inscripción no se borra: queda en BAJA con la fecha de hoy y sigue en el historial.
    @PostMapping("/{personaId}/inscripciones/{inscripcionId}/baja")
    public InscripcionCarreraResponse darDeBajaInscripcion(@PathVariable Long personaId,
                                                           @PathVariable Long inscripcionId) {
        return estudianteService.darDeBajaInscripcion(personaId, inscripcionId);
    }
}
