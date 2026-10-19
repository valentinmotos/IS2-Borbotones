package com.example.cliente.controller;

import com.example.cliente.dto.PersonaDTO;
import com.example.cliente.service.PersonaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/personas")
public class PersonaController extends BaseController<PersonaDTO> {
    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        super(personaService, "personas", "personas", "persona");
        this.personaService = personaService;
    }

    @Override
    protected void cargarDatosFormulario(Model model) {
        model.addAttribute("domicilios", personaService.listarDomicilios());
    }
}
