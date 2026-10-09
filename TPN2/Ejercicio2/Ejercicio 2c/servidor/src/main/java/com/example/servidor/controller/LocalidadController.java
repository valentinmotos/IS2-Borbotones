package com.example.servidor.controller;

import com.example.servidor.model.Localidad;
import com.example.servidor.service.LocalidadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/localidades")
public class LocalidadController {
    private final LocalidadService localidadService;

    public LocalidadController(LocalidadService localidadService) {
        this.localidadService = localidadService;
    }

    @GetMapping
    public List<Localidad> listar() {
        return localidadService.listar();
    }

    @GetMapping("/{id}")
    public Localidad buscar(@PathVariable Long id) {
        return localidadService.buscar(id);
    }
}
