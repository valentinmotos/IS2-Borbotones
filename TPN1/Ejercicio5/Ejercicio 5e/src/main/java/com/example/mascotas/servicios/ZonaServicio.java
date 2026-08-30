package com.example.mascotas.servicios;

import com.example.mascotas.dto.DTOConversor;
import com.example.mascotas.dto.ZonaDTO;
import com.example.mascotas.repositorios.ZonaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZonaServicio {

    @Autowired
    private ZonaRepositorio zonaRepositorio;

    @Autowired
    private DTOConversor dtoConversor;

    public List<ZonaDTO> listar() {
        return zonaRepositorio.findAll().stream()
                .map(dtoConversor::convertir)
                .toList();
    }
}
