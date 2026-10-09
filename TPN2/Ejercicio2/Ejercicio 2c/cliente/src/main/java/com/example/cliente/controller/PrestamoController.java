package com.example.cliente.controller;

import com.example.cliente.dto.PrestamoDTO;
import com.example.cliente.service.PrestamoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/prestamos")
public class PrestamoController {
    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("prestamos", prestamoService.listar());
        return "prestamos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        cargarFormulario(model, prestamoService.nuevo());
        return "prestamos/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        cargarFormulario(model, prestamoService.buscar(id));
        return "prestamos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute PrestamoDTO prestamo) {
        prestamoService.guardar(prestamo);
        return "redirect:/prestamos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        prestamoService.eliminar(id);
        return "redirect:/prestamos";
    }

    private void cargarFormulario(Model model, PrestamoDTO prestamo) {
        model.addAttribute("prestamo", prestamo);
        model.addAttribute("libros", prestamoService.listarLibros());
        model.addAttribute("personas", prestamoService.listarPersonas());
    }
}
