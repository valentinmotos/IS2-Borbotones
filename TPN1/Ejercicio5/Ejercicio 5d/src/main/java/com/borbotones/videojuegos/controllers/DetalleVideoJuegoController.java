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
public class DetalleVideoJuegoController {

    @Autowired
    private VideojuegoService videojuegoService;
    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private EstudioService estudioService;
    
    @GetMapping("/detalleVideoJuego/{id}")
        public String detalleVideoJuego(@PathVariable Long id, Model model) throws Exception {
            Videojuego videojuego = videojuegoService.findById(id);
            model.addAttribute("videojuego", videojuego);
            return "view/view_detalleVideoJuego";
        }
}
