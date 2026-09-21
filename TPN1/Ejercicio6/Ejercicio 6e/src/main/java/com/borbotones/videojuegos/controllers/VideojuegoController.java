package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.CategoriaService;
import com.borbotones.videojuegos.services.EstudioService;
import com.borbotones.videojuegos.services.ImagenService;
import com.borbotones.videojuegos.services.VideojuegoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideojuegoController {
    private final VideojuegoService videojuegoService;
    private final CategoriaService categoriaService;
    private final EstudioService estudioService;
    private final ImagenService imagenService;

    public VideojuegoController(
            VideojuegoService videojuegoService,
            CategoriaService categoriaService,
            EstudioService estudioService,
            ImagenService imagenService
    ) {
        this.videojuegoService = videojuegoService;
        this.categoriaService = categoriaService;
        this.estudioService = estudioService;
        this.imagenService = imagenService;
    }

    @GetMapping("/altaVideojuego")
    public String altaVideojuego(Model model) {
        model.addAttribute("videojuego", new Videojuego());
        prepararFormulario(model, "alta");
        return "view/videojuego/editVideojuego";
    }

    @PostMapping("/altaVideojuego")
    public String guardarVideojuego(
            @RequestParam("archivo") MultipartFile archivo,
            @Valid @ModelAttribute("videojuego") Videojuego videojuego,
            BindingResult result,
            Model model
    ) {
        prepararFormulario(model, "alta");
        if (result.hasErrors()) {
            return "view/videojuego/editVideojuego";
        }

        try {
            videojuego.setImagen(imagenService.guardar(archivo, true));
            videojuegoService.saveOne(videojuego);
            return "redirect:/inicio";
        } catch (IllegalArgumentException ex) {
            result.reject("videojuego.invalido", ex.getMessage());
            return "view/videojuego/editVideojuego";
        } catch (Exception ex) {
            model.addAttribute("error", "No fue posible guardar el videojuego");
            return "error";
        }
    }

    @GetMapping("/modificarVideojuego")
    public String modificarVideojuego(@RequestParam("id") long id, Model model) {
        try {
            model.addAttribute("videojuego", videojuegoService.findById(id));
            prepararFormulario(model, "modificar");
            return "view/videojuego/editVideojuego";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    @PostMapping("/videojuego/aceptarEditVideojuego")
    public String aceptarEditVideojuego(
            @RequestParam("archivo") MultipartFile archivo,
            @Valid @ModelAttribute("videojuego") Videojuego videojuego,
            BindingResult result,
            Model model
    ) {
        prepararFormulario(model, "modificar");
        if (result.hasErrors()) {
            return "view/videojuego/editVideojuego";
        }

        try {
            videojuego.setImagen(imagenService.guardar(archivo, false));
            videojuegoService.updateOne(videojuego, videojuego.getId());
            return "redirect:/inicio";
        } catch (IllegalArgumentException ex) {
            result.reject("videojuego.invalido", ex.getMessage());
            return "view/videojuego/editVideojuego";
        } catch (Exception ex) {
            model.addAttribute("error", "No fue posible actualizar el videojuego");
            return "error";
        }
    }

    @GetMapping("/consultarVideojuego")
    public String consultarVideojuego(@RequestParam("id") long id, Model model) {
        try {
            model.addAttribute("videojuego", videojuegoService.findByIdAndActivo(id));
            prepararFormulario(model, "consulta");
            return "view/videojuego/editVideojuego";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    @GetMapping("/bajaVideojuego")
    public String bajaVideojuego(@RequestParam("id") long id, Model model) {
        try {
            model.addAttribute("videojuego", videojuegoService.findById(id));
            return "view/videojuego/bajaVideojuego";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    @PostMapping("/bajaVideojuego")
    public String confirmarBajaVideojuego(@RequestParam("id") long id, Model model) {
        try {
            videojuegoService.deleteById(id);
            return "redirect:/inicio";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    @GetMapping("/busqueda")
    public String buscar(@RequestParam(name = "query", defaultValue = "") String query, Model model) {
        try {
            model.addAttribute("videojuegos", videojuegoService.findByTitle(query));
            model.addAttribute("consulta", query.trim());
            return "view/inicio";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    private void prepararFormulario(Model model, String modo) {
        model.addAttribute("categorias", categoriaService.findAllActivas());
        model.addAttribute("estudios", estudioService.findAllActivos());
        model.addAttribute("modo", modo);
    }
}
