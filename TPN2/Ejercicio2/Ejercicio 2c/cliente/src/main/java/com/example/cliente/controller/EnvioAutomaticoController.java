package com.example.cliente.controller;

import com.example.cliente.dto.EnvioAutomaticoDTO;
import com.example.cliente.service.EnvioAutomaticoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/envios-automaticos")
public class EnvioAutomaticoController {
    private final EnvioAutomaticoService envioAutomaticoService;

    public EnvioAutomaticoController(EnvioAutomaticoService envioAutomaticoService) {
        this.envioAutomaticoService = envioAutomaticoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("envios", envioAutomaticoService.listar());
        return "envios-automaticos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("envio", envioAutomaticoService.nuevo());
        return "envios-automaticos/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("envio", envioAutomaticoService.buscar(id));
        return "envios-automaticos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute EnvioAutomaticoDTO envio) {
        envioAutomaticoService.guardar(envio);
        return "redirect:/envios-automaticos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        envioAutomaticoService.eliminar(id);
        return "redirect:/envios-automaticos";
    }
}
