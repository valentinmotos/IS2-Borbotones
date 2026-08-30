package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.CategoriaService;
import com.borbotones.videojuegos.services.EstudioService;
import com.borbotones.videojuegos.services.VideojuegoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;

@Controller
public class JuegosExploradorController {

    @Autowired
    private VideojuegoService videojuegoService;
    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private EstudioService estudioService;

    /* ---- Alta ---- */

    @GetMapping({"/", "/exploradorJuegos"})
        public String exploradorJuegos(Model model) {
            try {

                List<Videojuego> videojuegos = videojuegoService.findAllByActivo();

                model.addAttribute("videojuegos", videojuegos);
                
                for (var campo : Videojuego.class.getDeclaredFields()) {
                    System.out.println(campo.getName());
                }
                
                return "view/view_videojuegos";

            } catch (Exception e) {

                model.addAttribute("error", e.getMessage());

                return "error";
            }
        }
}
