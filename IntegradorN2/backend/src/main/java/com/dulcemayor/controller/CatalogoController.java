package com.dulcemayor.controller;

import com.dulcemayor.dto.ProductoCatalogoDTO;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.service.CatalogoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/catalogo")
public class CatalogoController {

    private final CatalogoService catalogo;

    public CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public List<ProductoCatalogoDTO> buscar(@RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String categoriaId) {
        return catalogo.buscar(nombre, categoriaId);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return catalogo.listarCategorias();
    }
}
