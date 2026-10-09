package com.example.cliente.controller;

import com.example.cliente.dto.LibroDTO;
import com.example.cliente.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/libros")
public class LibroController {
    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("libros", libroService.listar());
        return "libros/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        cargarFormulario(model, libroService.nuevo());
        return "libros/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        cargarFormulario(model, libroService.buscar(id));
        return "libros/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute LibroDTO libro) {
        libroService.guardar(libro);
        return "redirect:/libros";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return "redirect:/libros";
    }

    private void cargarFormulario(Model model, LibroDTO libro) {
        model.addAttribute("libro", libro);
        model.addAttribute("personas", libroService.listarPersonas());
    }
}
