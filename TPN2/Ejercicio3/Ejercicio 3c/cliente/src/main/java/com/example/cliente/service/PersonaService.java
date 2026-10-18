package com.example.cliente.service;

import com.example.cliente.dao.PersonaDAO;
import com.example.cliente.dto.DomicilioDTO;
import com.example.cliente.dto.PersonaDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonaService extends BaseService<PersonaDTO> {
    private final DomicilioService domicilioService;

    public PersonaService(PersonaDAO personaDAO, DomicilioService domicilioService) {
        super(personaDAO);
        this.domicilioService = domicilioService;
    }

    public List<DomicilioDTO> listarDomicilios() {
        return domicilioService.listar();
    }

    @Override
    protected PersonaDTO crearDTO() {
        return new PersonaDTO();
    }

    @Override
    protected void inicializarRelaciones(PersonaDTO persona) {
        persona.setDomicilio(new DomicilioDTO());
    }
}
