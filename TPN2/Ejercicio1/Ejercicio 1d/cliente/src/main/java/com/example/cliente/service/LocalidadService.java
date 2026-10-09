package com.example.cliente.service;

import com.example.cliente.dao.LocalidadDAO;
import com.example.cliente.dto.LocalidadDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalidadService {
    private final LocalidadDAO localidadDAO;

    public LocalidadService(LocalidadDAO localidadDAO) {
        this.localidadDAO = localidadDAO;
    }

    public List<LocalidadDTO> listar() {
        return localidadDAO.listar();
    }
}
