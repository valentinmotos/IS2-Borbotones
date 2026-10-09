package com.example.cliente.service;

import com.example.cliente.dao.DomicilioDAO;
import com.example.cliente.dto.DomicilioDTO;
import com.example.cliente.dto.LocalidadDTO;
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

    public List<DomicilioDTO> listar() {
        return domicilioDAO.listar();
    }

    public DomicilioDTO buscar(Long id) {
        return domicilioDAO.buscar(id);
    }

    public DomicilioDTO nuevo() {
        DomicilioDTO domicilio = new DomicilioDTO();
        domicilio.setLocalidad(new LocalidadDTO());
        return domicilio;
    }

    public void guardar(DomicilioDTO domicilio) {
        domicilioDAO.guardar(domicilio);
    }

    public void eliminar(Long id) {
        domicilioDAO.eliminar(id);
    }

    public List<LocalidadDTO> listarLocalidades() {
        return localidadService.listar();
    }
}
