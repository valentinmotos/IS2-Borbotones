package com.example.cliente.service;

import com.example.cliente.dao.LibroDAO;
import com.example.cliente.dto.LibroDTO;
import com.example.cliente.dto.PersonaDTO;
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

    public List<LibroDTO> listar() {
        return libroDAO.listar();
    }

    public LibroDTO buscar(Long id) {
        return libroDAO.buscar(id);
    }

    public LibroDTO nuevo() {
        LibroDTO libro = new LibroDTO();
        libro.setPersona(new PersonaDTO());
        return libro;
    }

    public void guardar(LibroDTO libro) {
        libroDAO.guardar(libro);
    }

    public void eliminar(Long id) {
        libroDAO.eliminar(id);
    }

    public List<PersonaDTO> listarPersonas() {
        return personaService.listar();
    }
}
