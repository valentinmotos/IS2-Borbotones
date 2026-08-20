package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.services.ServicioCategoria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CategoriaController {

    @Autowired
    private ServicioCategoria svcCategoria;

    /* ---- Listado ---- */

    @GetMapping("/categorias")
    public String listarCategorias(Model model) {
        try {
            List<Categoria> categorias = svcCategoria.findAll();
            model.addAttribute("categorias", categorias);
            return "view/categoria/categoriaList";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Alta ---- */

    @GetMapping("/altaCategoria")
    public String altaCategoria(Model model) {
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("modo", "alta");
        return "view/categoria/categoriaEdit";
    }

    @PostMapping("/altaCategoria")
    public String guardarCategoria(@ModelAttribute("categoria") Categoria categoria, Model model) {
        try {
            svcCategoria.saveOne(categoria);
            return "redirect:/categorias";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Modificar ---- */

    @GetMapping("/modificarCategoria")
    public String modificarCategoria(@RequestParam("id") long id, Model model) {
        try {
            Categoria categoria = svcCategoria.findById(id);
            model.addAttribute("categoria", categoria);
            model.addAttribute("modo", "modificar");
            return "view/categoria/categoriaEdit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/categoria/aceptarEditCategoria")
    public String aceptarEditCategoria(@ModelAttribute("categoria") Categoria categoria, Model model) {
        try {
            svcCategoria.updateOne(categoria, categoria.getId());
            return "redirect:/categorias";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Consultar ---- */

    @GetMapping("/consultarCategoria")
    public String consultarCategoria(@RequestParam("id") long id, Model model) {
        try {
            Categoria categoria = svcCategoria.findById(id);
            model.addAttribute("categoria", categoria);
            model.addAttribute("modo", "consulta");
            return "view/categoria/categoriaEdit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Baja lógica ---- */

    @GetMapping("/bajaCategoria")
    public String bajaCategoria(@RequestParam("id") long id, Model model) {
        try {
            svcCategoria.deleteById(id);
            return "redirect:/categorias";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}
