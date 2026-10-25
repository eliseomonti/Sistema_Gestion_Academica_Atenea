package com.utn.atenea.personas.controller;

import com.utn.atenea.personas.dto.DatosPersonalesRequest;
import com.utn.atenea.personas.dto.PersonaResponse;
import com.utn.atenea.personas.service.PersonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    // Antes de un alta, Angular consulta el DNI: si la persona existe (404 si no) ofrece darle
    // el rol que le falta en lugar de cargarla de nuevo.
    @GetMapping("/dni/{dni}")
    public PersonaResponse buscarPorDni(@PathVariable String dni) {
        return personaService.buscarPorDni(dni);
    }

    // Los datos personales son de la persona, no del rol: un solo endpoint para estudiantes y docentes.
    @PutMapping("/{personaId}")
    public PersonaResponse modificar(@PathVariable Long personaId, @Valid @RequestBody DatosPersonalesRequest request) {
        return personaService.modificar(personaId, request);
    }

    // Baja lógica de la persona con todos sus roles: no se borra, queda inactiva y su usuario deja de poder entrar.
    @PostMapping("/{personaId}/baja")
    public PersonaResponse darDeBaja(@PathVariable Long personaId) {
        return personaService.darDeBaja(personaId);
    }
}
