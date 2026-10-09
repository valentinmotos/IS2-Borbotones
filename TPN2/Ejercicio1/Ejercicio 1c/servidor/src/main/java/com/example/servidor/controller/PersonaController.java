package com.example.servidor.controller;

import com.example.servidor.model.Persona;
import com.example.servidor.service.PersonaService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {
    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @GetMapping
    public List<Persona> listar() {
        return personaService.listar();
    }

    @GetMapping("/{id}")
    public Persona buscar(@PathVariable Long id) {
        return personaService.buscar(id);
    }

    @PostMapping
    public Persona crear(@RequestBody Persona persona) {
        return personaService.guardar(persona);
    }

    @PutMapping("/{id}")
    public Persona actualizar(@PathVariable Long id, @RequestBody Persona persona) {
        persona.setId(id);
        return personaService.guardar(persona);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        personaService.eliminar(id);
    }
}
