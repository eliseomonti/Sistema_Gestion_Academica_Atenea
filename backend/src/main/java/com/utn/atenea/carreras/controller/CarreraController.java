package com.utn.atenea.carreras.controller;

import com.utn.atenea.carreras.dto.CarreraResponse;
import com.utn.atenea.carreras.service.CarreraService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carreras")
@RequiredArgsConstructor
public class CarreraController {

    private final CarreraService carreraService;

    @GetMapping
    public List<CarreraResponse> listar() {
        return carreraService.listar();
    }
}
