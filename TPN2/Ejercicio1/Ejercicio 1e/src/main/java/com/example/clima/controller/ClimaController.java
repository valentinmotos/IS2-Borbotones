package com.example.clima.controller;

import com.example.clima.dto.ClimaDTO;
import com.example.clima.service.ClimaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clima")
public class ClimaController {
    private final ClimaService climaService;

    public ClimaController(ClimaService climaService) {
        this.climaService = climaService;
    }

    @GetMapping
    public ClimaDTO buscar(@RequestParam String ciudad) {
        return climaService.buscarPorCiudad(ciudad);
    }
}
