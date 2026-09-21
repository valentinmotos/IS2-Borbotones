package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Estudio;
import com.borbotones.videojuegos.services.EstudioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EstudioController {
    private final EstudioService estudioService;

    public EstudioController(EstudioService estudioService) {
        this.estudioService = estudioService;
    }

    @GetMapping("/estudios")
    public String listarEstudios(Model model) {
        model.addAttribute("estudios", estudioService.findAll());
        return "view/estudio/estudioList";
    }

    @GetMapping("/altaEstudio")
    public String altaEstudio(Model model) {
        model.addAttribute("estudio", new Estudio());
        model.addAttribute("modo", "alta");
        return "view/estudio/estudioEdit";
    }

    @PostMapping("/altaEstudio")
    public String guardarEstudio(
            @Valid @ModelAttribute("estudio") Estudio estudio,
            BindingResult result,
            Model model
    ) {
        model.addAttribute("modo", "alta");
        if (result.hasErrors()) {
            return "view/estudio/estudioEdit";
        }
        estudioService.saveOne(estudio);
        return "redirect:/estudios";
    }

    @GetMapping("/modificarEstudio")
    public String modificarEstudio(@RequestParam("id") long id, Model model) {
        model.addAttribute("estudio", estudioService.findById(id));
        model.addAttribute("modo", "modificar");
        return "view/estudio/estudioEdit";
    }

    @PostMapping("/estudio/aceptarEditEstudio")
    public String aceptarEditEstudio(
            @Valid @ModelAttribute("estudio") Estudio estudio,
            BindingResult result,
            Model model
    ) {
        model.addAttribute("modo", "modificar");
        if (result.hasErrors()) {
            return "view/estudio/estudioEdit";
        }
        estudioService.updateOne(estudio, estudio.getId());
        return "redirect:/estudios";
    }

    @GetMapping("/consultarEstudio")
    public String consultarEstudio(@RequestParam("id") long id, Model model) {
        model.addAttribute("estudio", estudioService.findById(id));
        model.addAttribute("modo", "consulta");
        return "view/estudio/estudioEdit";
    }

    @PostMapping("/bajaEstudio")
    public String bajaEstudio(@RequestParam("id") long id) {
        estudioService.deleteById(id);
        return "redirect:/estudios";
    }
}
