package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Estudio;
import com.borbotones.videojuegos.services.ServicioEstudio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class EstudioController {

    @Autowired
    private ServicioEstudio svcEstudio;

    /* ---- Listado ---- */

    @GetMapping("/estudios")
    public String listarEstudios(Model model) {
        try {
            List<Estudio> estudios = svcEstudio.findAll();
            model.addAttribute("estudios", estudios);
            return "view/estudio/estudioList";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Alta ---- */

    @GetMapping("/altaEstudio")
    public String altaEstudio(Model model) {
        model.addAttribute("estudio", new Estudio());
        model.addAttribute("modo", "alta");
        return "view/estudio/estudioEdit";
    }

    @PostMapping("/altaEstudio")
    public String guardarEstudio(@ModelAttribute("estudio") Estudio estudio, Model model) {
        try {
            svcEstudio.saveOne(estudio);
            return "redirect:/estudios";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Modificar ---- */

    @GetMapping("/modificarEstudio")
    public String modificarEstudio(@RequestParam("id") long id, Model model) {
        try {
            Estudio estudio = svcEstudio.findById(id);
            model.addAttribute("estudio", estudio);
            model.addAttribute("modo", "modificar");
            return "view/estudio/estudioEdit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/estudio/aceptarEditEstudio")
    public String aceptarEditEstudio(@ModelAttribute("estudio") Estudio estudio, Model model) {
        try {
            svcEstudio.updateOne(estudio, estudio.getId());
            return "redirect:/estudios";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Consultar ---- */

    @GetMapping("/consultarEstudio")
    public String consultarEstudio(@RequestParam("id") long id, Model model) {
        try {
            Estudio estudio = svcEstudio.findById(id);
            model.addAttribute("estudio", estudio);
            model.addAttribute("modo", "consulta");
            return "view/estudio/estudioEdit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /* ---- Baja lógica ---- */

    @GetMapping("/bajaEstudio")
    public String bajaEstudio(@RequestParam("id") long id, Model model) {
        try {
            svcEstudio.deleteById(id);
            return "redirect:/estudios";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}
