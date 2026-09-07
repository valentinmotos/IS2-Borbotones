package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.services.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/categorias")
    public String listarCategorias(Model model) {
        model.addAttribute("categorias", categoriaService.findAll());
        return "view/categoria/categoriaList";
    }

    @GetMapping("/altaCategoria")
    public String altaCategoria(Model model) {
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("modo", "alta");
        return "view/categoria/categoriaEdit";
    }

    @PostMapping("/altaCategoria")
    public String guardarCategoria(
            @Valid @ModelAttribute("categoria") Categoria categoria,
            BindingResult result,
            Model model
    ) {
        model.addAttribute("modo", "alta");
        if (result.hasErrors()) {
            return "view/categoria/categoriaEdit";
        }
        categoriaService.saveOne(categoria);
        return "redirect:/categorias";
    }

    @GetMapping("/modificarCategoria")
    public String modificarCategoria(@RequestParam("id") long id, Model model) {
        model.addAttribute("categoria", categoriaService.findById(id));
        model.addAttribute("modo", "modificar");
        return "view/categoria/categoriaEdit";
    }

    @PostMapping("/categoria/aceptarEditCategoria")
    public String aceptarEditCategoria(
            @Valid @ModelAttribute("categoria") Categoria categoria,
            BindingResult result,
            Model model
    ) {
        model.addAttribute("modo", "modificar");
        if (result.hasErrors()) {
            return "view/categoria/categoriaEdit";
        }
        categoriaService.updateOne(categoria, categoria.getId());
        return "redirect:/categorias";
    }

    @GetMapping("/consultarCategoria")
    public String consultarCategoria(@RequestParam("id") long id, Model model) {
        model.addAttribute("categoria", categoriaService.findById(id));
        model.addAttribute("modo", "consulta");
        return "view/categoria/categoriaEdit";
    }

    @PostMapping("/bajaCategoria")
    public String bajaCategoria(@RequestParam("id") long id) {
        categoriaService.deleteById(id);
        return "redirect:/categorias";
    }
}
