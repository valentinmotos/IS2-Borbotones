package com.example.servidor.service;

import com.example.servidor.dao.PersonaDAO;
import com.example.servidor.model.Domicilio;
import com.example.servidor.model.Persona;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonaService extends BaseService<Persona> {
    private final PersonaDAO personaDAO;
    private final DomicilioService domicilioService;

    public PersonaService(PersonaDAO personaDAO, DomicilioService domicilioService) {
        super(personaDAO, "Persona no encontrada");
        this.personaDAO = personaDAO;
        this.domicilioService = domicilioService;
    }

    public List<Persona> listarConFechaNacimiento() {
        return personaDAO.findByFechaNacimientoIsNotNull();
    }

    @Override
    protected void completarRelaciones(Persona persona) {
        Domicilio domicilio = persona.getDomicilio();
        persona.setDomicilio(domicilio != null && domicilio.getId() != null ? domicilioService.buscar(domicilio.getId()) : null);
    }
}
