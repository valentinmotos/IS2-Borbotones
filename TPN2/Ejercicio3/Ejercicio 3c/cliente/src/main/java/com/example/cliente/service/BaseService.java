package com.example.cliente.service;

import com.example.cliente.dao.BaseDAO;
import com.example.cliente.dto.BaseDTO;

import java.util.List;

public abstract class BaseService<D extends BaseDTO> {
    protected final BaseDAO<D> dao;

    protected BaseService(BaseDAO<D> dao) {
        this.dao = dao;
    }

    public List<D> listar() {
        return dao.listar();
    }

    public D buscar(Long id) {
        return dao.buscar(id);
    }

    public final D nuevo() {
        D dto = crearDTO();
        inicializarRelaciones(dto);
        asignarValoresPorDefecto(dto);
        return dto;
    }

    public void guardar(D dto) {
        dao.guardar(dto);
    }

    public void eliminar(Long id) {
        dao.eliminar(id);
    }

    protected abstract D crearDTO();

    protected void inicializarRelaciones(D dto) {
    }

    protected void asignarValoresPorDefecto(D dto) {
    }
}
