package com.example.servidor.service;

import com.example.servidor.dao.LibroDAO;
import com.example.servidor.model.Libro;
import com.example.servidor.model.Persona;
import org.springframework.stereotype.Service;

@Service
public class LibroService extends BaseService<Libro> {
    private final PersonaService personaService;

    public LibroService(LibroDAO libroDAO, PersonaService personaService) {
        super(libroDAO, "Libro no encontrado");
        this.personaService = personaService;
    }

    @Override
    protected void completarRelaciones(Libro libro) {
        Persona persona = libro.getPersona();
        libro.setPersona(persona != null && persona.getId() != null ? personaService.buscar(persona.getId()) : null);
    }
}
