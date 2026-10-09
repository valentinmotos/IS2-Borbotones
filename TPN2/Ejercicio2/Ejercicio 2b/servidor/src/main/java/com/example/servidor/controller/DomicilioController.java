package com.example.servidor.controller;

import com.example.servidor.model.Domicilio;
import com.example.servidor.service.DomicilioService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/domicilios")
public class DomicilioController {
    private final DomicilioService domicilioService;

    public DomicilioController(DomicilioService domicilioService) {
        this.domicilioService = domicilioService;
    }

    @GetMapping
    public List<Domicilio> listar() {
        return domicilioService.listar();
    }

    @GetMapping("/{id}")
    public Domicilio buscar(@PathVariable Long id) {
        return domicilioService.buscar(id);
    }

    @PostMapping
    public Domicilio crear(@RequestBody Domicilio domicilio) {
        return domicilioService.guardar(domicilio);
    }

    @PutMapping("/{id}")
    public Domicilio actualizar(@PathVariable Long id, @RequestBody Domicilio domicilio) {
        domicilio.setId(id);
        return domicilioService.guardar(domicilio);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        domicilioService.eliminar(id);
    }
}
