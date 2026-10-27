package com.example.servidor.service;

import com.example.servidor.dao.LocalidadDAO;
import com.example.servidor.model.Localidad;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalidadService {
    private final LocalidadDAO localidadDAO;

    public LocalidadService(LocalidadDAO localidadDAO) {
        this.localidadDAO = localidadDAO;
    }

    public List<Localidad> listar() {
        return localidadDAO.findAll();
    }

    public Localidad buscar(Long id) {
        if (id == null) {
            return null;
        }
        return localidadDAO.findById(id).orElse(null);
    }

    public Localidad guardar(Localidad localidad) {
        return localidadDAO.save(localidad);
    }

    public boolean existeDenominacion(String denominacion) {
        return localidadDAO.existsByDenominacion(denominacion);
    }
}
