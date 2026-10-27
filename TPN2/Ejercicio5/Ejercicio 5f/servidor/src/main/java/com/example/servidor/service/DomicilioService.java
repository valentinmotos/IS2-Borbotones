package com.example.servidor.service;

import com.example.servidor.dao.DomicilioDAO;
import com.example.servidor.model.Domicilio;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DomicilioService {
    private final DomicilioDAO domicilioDAO;
    private final LocalidadService localidadService;

    public DomicilioService(DomicilioDAO domicilioDAO, LocalidadService localidadService) {
        this.domicilioDAO = domicilioDAO;
        this.localidadService = localidadService;
    }

    public List<Domicilio> listar() {
        return domicilioDAO.findAll();
    }

    public Domicilio buscar(Long id) {
        return domicilioDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Domicilio no encontrado"));
    }

    public Domicilio guardar(Domicilio domicilio) {
        if (domicilio.getLocalidad() != null && domicilio.getLocalidad().getId() != null) {
            domicilio.setLocalidad(localidadService.buscar(domicilio.getLocalidad().getId()));
        }
        return domicilioDAO.save(domicilio);
    }

    public void eliminar(Long id) {
        domicilioDAO.deleteById(id);
    }
}
