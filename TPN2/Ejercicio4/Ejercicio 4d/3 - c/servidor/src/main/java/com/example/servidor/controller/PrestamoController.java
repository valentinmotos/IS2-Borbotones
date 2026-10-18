package com.example.servidor.controller;

import com.example.servidor.model.Prestamo;
import com.example.servidor.service.PrestamoService;
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
@RequestMapping("/api/prestamos")
public class PrestamoController {
    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @GetMapping
    public List<Prestamo> listar() {
        return prestamoService.listar();
    }

    @GetMapping("/{id}")
    public Prestamo buscar(@PathVariable Long id) {
        return prestamoService.buscar(id);
    }

    @PostMapping
    public Prestamo crear(@RequestBody Prestamo prestamo) {
        return prestamoService.guardar(prestamo);
    }

    @PutMapping("/{id}")
    public Prestamo actualizar(@PathVariable Long id, @RequestBody Prestamo prestamo) {
        prestamo.setId(id);
        return prestamoService.guardar(prestamo);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        prestamoService.eliminar(id);
    }
}
