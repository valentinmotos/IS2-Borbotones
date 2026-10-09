package com.example.cliente.service;

import com.example.cliente.dao.PersonaDAO;
import com.example.cliente.dto.DomicilioDTO;
import com.example.cliente.dto.PersonaDTO;
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

    public List<PersonaDTO> listar() {
        return personaDAO.listar();
    }

    public PersonaDTO buscar(Long id) {
        return personaDAO.buscar(id);
    }

    public PersonaDTO nuevo() {
        PersonaDTO persona = new PersonaDTO();
        persona.setDomicilio(new DomicilioDTO());
        return persona;
    }

    public void guardar(PersonaDTO persona) {
        personaDAO.guardar(persona);
    }

    public void eliminar(Long id) {
        personaDAO.eliminar(id);
    }

    public List<DomicilioDTO> listarDomicilios() {
        return domicilioService.listar();
    }
}
