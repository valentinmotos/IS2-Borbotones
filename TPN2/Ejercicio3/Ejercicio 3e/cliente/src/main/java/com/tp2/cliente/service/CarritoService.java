package com.tp2.cliente.service;

import com.tp2.cliente.dao.CarritoDao;
import com.tp2.cliente.dto.CarritoDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarritoService {
    private final CarritoDao carritoDao;

    public CarritoService(CarritoDao carritoDao) {
        this.carritoDao = carritoDao;
    }

    public List<CarritoDto> listar() {
        return carritoDao.listar();
    }

    public void crear(String usuarioId) {
        carritoDao.crear(usuarioId);
    }

    public void agregarArticulo(String carritoId, String articuloId, int cantidad) {
        carritoDao.agregarArticulo(carritoId, articuloId, cantidad);
    }

    public void editarDetalle(String carritoId, String detalleId, int cantidad) {
        carritoDao.editarDetalle(carritoId, detalleId, cantidad);
    }

    public void eliminar(String carritoId) {
        carritoDao.eliminar(carritoId);
    }

    public void finalizar(String carritoId) {
        carritoDao.finalizar(carritoId);
    }
}
