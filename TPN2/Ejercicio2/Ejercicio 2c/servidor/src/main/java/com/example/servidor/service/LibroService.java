package com.example.servidor.service;

import com.example.servidor.dao.LibroDAO;
import com.example.servidor.model.Libro;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibroService {
    private final LibroDAO libroDAO;
    private final PersonaService personaService;

    public LibroService(LibroDAO libroDAO, PersonaService personaService) {
        this.libroDAO = libroDAO;
        this.personaService = personaService;
    }

    public List<Libro> listar() {
        return libroDAO.findAll();
    }

    public Libro buscar(Long id) {
        return libroDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Libro no encontrado"));
    }

    public Libro guardar(Libro libro) {
        if (libro.getPersona() != null && libro.getPersona().getId() != null) {
            libro.setPersona(personaService.buscar(libro.getPersona().getId()));
        }
        return libroDAO.save(libro);
    }

    public void eliminar(Long id) {
        libroDAO.deleteById(id);
    }
}
