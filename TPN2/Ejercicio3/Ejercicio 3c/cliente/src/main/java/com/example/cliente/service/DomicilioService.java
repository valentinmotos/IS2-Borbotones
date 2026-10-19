package com.example.cliente.service;

import com.example.cliente.dao.DomicilioDAO;
import com.example.cliente.dto.DomicilioDTO;
import com.example.cliente.dto.LocalidadDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DomicilioService extends BaseService<DomicilioDTO> {
    private final LocalidadService localidadService;

    public DomicilioService(DomicilioDAO domicilioDAO, LocalidadService localidadService) {
        super(domicilioDAO);
        this.localidadService = localidadService;
    }

    public List<LocalidadDTO> listarLocalidades() {
        return localidadService.listar();
    }

    @Override
    protected DomicilioDTO crearDTO() {
        return new DomicilioDTO();
    }

    @Override
    protected void inicializarRelaciones(DomicilioDTO domicilio) {
        domicilio.setLocalidad(new LocalidadDTO());
    }
}
