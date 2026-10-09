package com.example.servidor.service;

import com.example.servidor.dao.PersonaDAO;
import com.example.servidor.model.Persona;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonaService {
    private final PersonaDAO personaDAO;
    private final DomicilioService domicilioService;

    public PersonaService(PersonaDAO personaDAO, DomicilioService domicilioService) {
        this.personaDAO = personaDAO;
        this.domicilioService = domicilioService;
    }

    public List<Persona> listar() {
        return personaDAO.findAll();
    }

    public Persona buscar(Long id) {
        return personaDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
    }

    public Persona guardar(Persona persona) {
        if (persona.getDomicilio() != null && persona.getDomicilio().getId() != null) {
            persona.setDomicilio(domicilioService.buscar(persona.getDomicilio().getId()));
        }
        return personaDAO.save(persona);
    }

    public void eliminar(Long id) {
        personaDAO.deleteById(id);
    }
}
