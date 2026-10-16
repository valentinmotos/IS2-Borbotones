package com.tp2.servidor.controller;

import com.tp2.servidor.dto.ArticuloRequest;
import com.tp2.servidor.entity.Articulo;
import com.tp2.servidor.service.ArticuloService;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articulos")
public class ArticuloController extends BaseRestController<Articulo, ArticuloRequest, String> {
    public ArticuloController(ArticuloService articuloService) {
        super(articuloService);
    }

    @Override
    protected Optional<Articulo> modificarRecurso(String id, ArticuloRequest request) {
        return Optional.empty();
    }

    @Override
    protected void eliminarRecurso(String id) {
        // La API de artículos no permite eliminar recursos.
    }
}
