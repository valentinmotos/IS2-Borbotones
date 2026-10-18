package com.example.servidor.controller;

import com.example.servidor.model.Persona;
import com.example.servidor.service.PersonaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personas")
public class PersonaController extends BaseController<Persona> {
    public PersonaController(PersonaService service) {
        super(service);
    }
}
