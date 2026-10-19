package com.example.servidor.service;

import com.example.servidor.dao.DomicilioDAO;
import com.example.servidor.model.Domicilio;
import com.example.servidor.model.Localidad;
import org.springframework.stereotype.Service;

@Service
public class DomicilioService extends BaseService<Domicilio> {
    private final LocalidadService localidadService;

    public DomicilioService(DomicilioDAO domicilioDAO, LocalidadService localidadService) {
        super(domicilioDAO, "Domicilio no encontrado");
        this.localidadService = localidadService;
    }

    @Override
    protected void completarRelaciones(Domicilio domicilio) {
        Localidad localidad = domicilio.getLocalidad();
        domicilio.setLocalidad(localidad != null && localidad.getId() != null ? localidadService.buscar(localidad.getId()) : null);
    }
}
