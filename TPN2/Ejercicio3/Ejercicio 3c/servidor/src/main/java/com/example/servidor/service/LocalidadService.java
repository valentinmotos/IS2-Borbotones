package com.example.servidor.service;

import com.example.servidor.dao.LocalidadDAO;
import com.example.servidor.model.Localidad;
import org.springframework.stereotype.Service;

@Service
public class LocalidadService extends BaseService<Localidad> {
    private final LocalidadDAO localidadDAO;

    public LocalidadService(LocalidadDAO localidadDAO) {
        super(localidadDAO, "Localidad no encontrada");
        this.localidadDAO = localidadDAO;
    }

    public boolean existeDenominacion(String denominacion) {
        return localidadDAO.existsByDenominacion(denominacion);
    }
}
