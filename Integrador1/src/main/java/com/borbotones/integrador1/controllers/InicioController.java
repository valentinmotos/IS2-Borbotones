package com.borbotones.integrador1.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping({"/", "/inicio"})
    public String inicio(Model model) {
        model.addAttribute("titulo", "TP Integrador 1");
        return "inicio";
    }
}
