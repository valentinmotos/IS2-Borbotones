package com.borbotones.videojuegos.controllers.rest;

import com.borbotones.videojuegos.controllers.BaseRestControllerTemplate;
import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.services.CategoriaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaRestController extends BaseRestControllerTemplate<Categoria> {
    public CategoriaRestController(CategoriaService service) {
        super(service);
    }
}
