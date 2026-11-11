package com.dulcemayor.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.dto.ProductoCatalogoDTO;
import com.dulcemayor.service.CatalogoService;

/**
 * Catálogo público (E3-01). Sin login: va bajo /api/publico (abierto en SecurityConfig).
 * Las fotos se piden a GET /api/imagenes/{imagenId}, también pública.
 */
@RestController
@RequestMapping("/api/publico/catalogo")
public class CatalogoController {

    private final CatalogoService catalogo;

    public CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public List<ProductoCatalogoDTO> buscar(@RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoriaId) {
        return catalogo.buscar(nombre, categoriaId);
    }

    @GetMapping("/{id}")
    public ProductoCatalogoDTO buscarDisponible(@PathVariable String id) {
        return catalogo.buscarDisponible(id);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return catalogo.listarCategorias();
    }
}
