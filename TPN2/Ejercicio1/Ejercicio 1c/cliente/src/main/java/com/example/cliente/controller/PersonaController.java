package com.example.cliente.controller;

import com.example.cliente.dto.PersonaDTO;
import com.example.cliente.service.PersonaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/personas")
public class PersonaController {
    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("personas", personaService.listar());
        return "personas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        cargarFormulario(model, personaService.nuevo());
        return "personas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        cargarFormulario(model, personaService.buscar(id));
        return "personas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute PersonaDTO persona) {
        personaService.guardar(persona);
        return "redirect:/personas";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        personaService.eliminar(id);
        return "redirect:/personas";
    }

    private void cargarFormulario(Model model, PersonaDTO persona) {
        model.addAttribute("persona", persona);
        model.addAttribute("domicilios", personaService.listarDomicilios());
    }
}
