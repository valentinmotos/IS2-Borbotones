package com.tp2.cliente.service;

import com.tp2.cliente.dao.ArticuloDao;
import com.tp2.cliente.dto.ArticuloDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticuloService {
    private final ArticuloDao articuloDao;

    public ArticuloService(ArticuloDao articuloDao) {
        this.articuloDao = articuloDao;
    }

    public List<ArticuloDto> listar() {
        return articuloDao.listar();
    }
}
