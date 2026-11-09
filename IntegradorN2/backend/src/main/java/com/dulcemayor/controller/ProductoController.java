package com.dulcemayor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.ProductoRequest;
import com.dulcemayor.dto.ProductoResponse;
import com.dulcemayor.service.ProductoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/productos")
public class ProductoController extends AbstractCrudController<ProductoRequest, ProductoResponse> {

    private final ProductoService productos;

    public ProductoController(ProductoService productos) {
        super(productos);
        this.productos = productos;
    }

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @ModelAttribute ProductoRequest request) {
        return productos.crear(request);
    }

    @Override
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse modificar(@PathVariable String id, @Valid @ModelAttribute ProductoRequest request) {
        return productos.modificar(id, request);
    }

    @GetMapping("/buscar")
    public List<ProductoResponse> buscarActivos(@RequestParam(defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "") String categoriaId) {
        return productos.buscarActivos(busqueda, categoriaId);
    }
}
