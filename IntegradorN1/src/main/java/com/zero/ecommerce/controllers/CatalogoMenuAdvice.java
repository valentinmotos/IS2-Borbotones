package com.zero.ecommerce.controllers;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zero.ecommerce.services.CategoriaService;

/** Expone el arbol activo a todas las pantallas que usan el layout publico. */
@ControllerAdvice(basePackages = {
        "com.zero.ecommerce.controllers.publico",
        "com.zero.ecommerce.controllers.cliente" })
public class CatalogoMenuAdvice {

    private final CategoriaService categoriaService;

    public CatalogoMenuAdvice(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @ModelAttribute("categoriasCatalogo")
    public Object categoriasCatalogo() {
        return categoriaService.listarArbolActivo();
    }
}
