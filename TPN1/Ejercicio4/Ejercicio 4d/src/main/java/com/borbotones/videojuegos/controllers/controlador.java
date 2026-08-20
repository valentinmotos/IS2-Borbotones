package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.ServicioVideojuego;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class controlador {

    @Autowired
    private ServicioVideojuego svcVideojuego;

    @GetMapping({"/", "/inicio"})
    public String inicio(Model model) {
        try {
            List<Videojuego> videojuegos = svcVideojuego.findAllByActivo();
            model.addAttribute("videojuegos", videojuegos);
            return "view/inicio";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}
