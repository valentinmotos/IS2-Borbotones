package com.tp2.servidor.service;

import com.tp2.servidor.dao.ArticuloDao;
import com.tp2.servidor.dto.ArticuloRequest;
import com.tp2.servidor.entity.Articulo;
import org.springframework.stereotype.Service;

@Service
public class ArticuloService extends BaseRestService<Articulo, ArticuloRequest, String> {
    public ArticuloService(ArticuloDao articuloDao, ImagenService imagenService, ProveedorService proveedorService) {
        super(articuloDao, request -> convertir(request, imagenService, proveedorService), articulo -> !articulo.isEliminado());
    }

    private static Articulo convertir(ArticuloRequest request, ImagenService imagenService, ProveedorService proveedorService) {
        Articulo articulo = new Articulo();
        articulo.setNombre(request.getNombre());
        articulo.setPrecio(request.getPrecio());
        if (request.getImagenId() != null) {
            articulo.setImagen(imagenService.buscarActivo(request.getImagenId()));
        }
        if (request.getProveedorId() != null) {
            articulo.setProveedor(proveedorService.buscarActivo(request.getProveedorId()));
        }
        return articulo;
    }

    @Override
    protected void validar(Operacion operacion, Articulo articulo) {
        if (operacion != Operacion.BAJA && articulo.getPrecio() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
    }

    @Override
    protected void actualizar(Articulo existente, Articulo nuevo) {
        existente.setNombre(nuevo.getNombre());
        existente.setPrecio(nuevo.getPrecio());
        existente.setImagen(nuevo.getImagen());
        existente.setProveedor(nuevo.getProveedor());
    }

    @Override
    protected void marcarEliminada(Articulo articulo) {
        articulo.setEliminado(true);
    }
}
