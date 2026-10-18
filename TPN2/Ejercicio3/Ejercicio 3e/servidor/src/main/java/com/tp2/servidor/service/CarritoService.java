package com.tp2.servidor.service;

import com.tp2.servidor.dao.CarritoDao;
import com.tp2.servidor.entity.Articulo;
import com.tp2.servidor.entity.Carrito;
import com.tp2.servidor.template.CarritoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarritoService extends CarritoTemplate {
    private final ArticuloService articuloService;

    public CarritoService(CarritoDao carritoDao, UsuarioService usuarioService, ArticuloService articuloService) {
        super(carritoDao, usuarioService::buscarActivo);
        this.articuloService = articuloService;
    }

    @Transactional
    public Carrito agregar(String carritoId, String articuloId, int cantidad) {
        return agregarArticulo(carritoId, articuloService.buscarActivo(articuloId), cantidad);
    }

    @Transactional
    public Carrito editar(String carritoId, String detalleId, int cantidad) {
        return editarDetalle(carritoId, detalleId, cantidad);
    }

    @Transactional
    public Carrito finalizar(String carritoId) {
        return finalizarCompra(carritoId);
    }

}
