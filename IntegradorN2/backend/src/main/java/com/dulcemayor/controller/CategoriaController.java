package com.dulcemayor.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.service.CategoriaService;

/**
 * ABM de categorías: hereda los endpoints de AbstractCrudController.
 * Acceso: JEFE y ADMINISTRATIVO (regla de /api/categorias/** en SecurityConfig).
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController extends AbstractCrudController<CategoriaRequest, CategoriaResponse> {

    public CategoriaController(CategoriaService categoriaService) {
        super(categoriaService);
    }
}
