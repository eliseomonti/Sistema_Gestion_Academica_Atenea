package com.utn.atenea.personas.controller;

import com.utn.atenea.personas.dto.AltaPersonaResponse;
import com.utn.atenea.personas.dto.DatosPersonalesRequest;
import com.utn.atenea.personas.dto.DocenteListadoResponse;
import com.utn.atenea.personas.dto.PersonaResponse;
import com.utn.atenea.personas.service.DocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/docentes")
@RequiredArgsConstructor
public class DocenteController {

    private final DocenteService docenteService;

    // Ejemplo: GET /api/admin/docentes?buscar=ruiz&page=0&size=20. Misma forma de respuesta que el listado de estudiantes.
    // Por defecto trae sólo los activos; con activo=false, los dados de baja.
    @GetMapping
    public PagedModel<DocenteListadoResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "true") boolean activo,
            @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(docenteService.listar(buscar, activo, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AltaPersonaResponse alta(@Valid @RequestBody DatosPersonalesRequest request) {
        return docenteService.alta(request);
    }

    // Estudiantes y docentes se identifican por el id de la persona: con el PUT, la persona que ya
    // existe pasa a estar también en /docentes.
    @PutMapping("/{personaId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse asignarRol(@PathVariable Long personaId) {
        return docenteService.asignarRol(personaId);
    }
}
