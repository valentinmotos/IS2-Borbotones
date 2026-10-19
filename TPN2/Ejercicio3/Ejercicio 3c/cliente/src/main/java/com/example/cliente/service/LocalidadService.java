package com.example.cliente.service;

import com.example.cliente.dao.LocalidadDAO;
import com.example.cliente.dto.LocalidadDTO;
import org.springframework.stereotype.Service;

@Service
public class LocalidadService extends BaseService<LocalidadDTO> {
    public LocalidadService(LocalidadDAO localidadDAO) {
        super(localidadDAO);
    }

    @Override
    protected LocalidadDTO crearDTO() {
        return new LocalidadDTO();
    }
}
