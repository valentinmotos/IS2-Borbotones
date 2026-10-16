package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.VideojuegoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class JuegosExploradorController {

    @Autowired
    private VideojuegoService videojuegoService;
    @GetMapping({"/", "/exploradorJuegos"})
        public String exploradorJuegos(Model model) {
            try {

                List<Videojuego> videojuegos = videojuegoService.findAllByActivo();

                model.addAttribute("videojuegos", videojuegos);
                return "view/view_videojuegos";

            } catch (Exception e) {

                model.addAttribute("error", e.getMessage());

                return "error";
            }
        }
}
