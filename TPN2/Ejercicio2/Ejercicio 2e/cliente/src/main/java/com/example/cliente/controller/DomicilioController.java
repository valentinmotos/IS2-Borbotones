package com.example.cliente.controller;

import com.example.cliente.dto.DomicilioDTO;
import com.example.cliente.service.DomicilioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/domicilios")
public class DomicilioController {
    private final DomicilioService domicilioService;

    public DomicilioController(DomicilioService domicilioService) {
        this.domicilioService = domicilioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("domicilios", domicilioService.listar());
        return "domicilios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        cargarFormulario(model, domicilioService.nuevo());
        return "domicilios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        cargarFormulario(model, domicilioService.buscar(id));
        return "domicilios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute DomicilioDTO domicilio) {
        domicilioService.guardar(domicilio);
        return "redirect:/domicilios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        domicilioService.eliminar(id);
        return "redirect:/domicilios";
    }

    private void cargarFormulario(Model model, DomicilioDTO domicilio) {
        model.addAttribute("domicilio", domicilio);
        model.addAttribute("localidades", domicilioService.listarLocalidades());
    }
}
