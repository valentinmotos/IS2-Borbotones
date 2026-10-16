package com.tp2.servidor.service;

import com.tp2.servidor.dao.ArticuloDao;
import com.tp2.servidor.dto.ArticuloRequest;
import com.tp2.servidor.entity.Articulo;
import org.springframework.stereotype.Service;

@Service
public class ArticuloService extends BaseRestService<Articulo, ArticuloRequest, String> {
    private final ArticuloDao articuloDao;

    public ArticuloService(ArticuloDao articuloDao) {
        super(articuloDao, ArticuloService::convertir, articulo -> !articulo.isEliminado());
        this.articuloDao = articuloDao;
    }

    private static Articulo convertir(ArticuloRequest request) {
        Articulo articulo = new Articulo();
        articulo.setNombre(request.getNombre());
        articulo.setPrecio(request.getPrecio());
        return articulo;
    }

    @Override
    protected void marcarEliminada(Articulo articulo) {
        articulo.setEliminado(true);
    }

    public Articulo buscarActivo(String id) {
        return buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Articulo no encontrado o eliminado"));
    }
}
