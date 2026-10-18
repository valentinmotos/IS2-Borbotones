package com.example.cliente.service;

import com.example.cliente.dao.LibroDAO;
import com.example.cliente.dto.LibroDTO;
import com.example.cliente.dto.PersonaDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibroService extends BaseService<LibroDTO> {
    private final PersonaService personaService;

    public LibroService(LibroDAO libroDAO, PersonaService personaService) {
        super(libroDAO);
        this.personaService = personaService;
    }

    public List<PersonaDTO> listarPersonas() {
        return personaService.listar();
    }

    @Override
    protected LibroDTO crearDTO() {
        return new LibroDTO();
    }

    @Override
    protected void inicializarRelaciones(LibroDTO libro) {
        libro.setPersona(new PersonaDTO());
    }
}
