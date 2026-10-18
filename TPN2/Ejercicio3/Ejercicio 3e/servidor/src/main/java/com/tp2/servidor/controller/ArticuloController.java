package com.tp2.servidor.controller;

import com.tp2.servidor.dto.ArticuloRequest;
import com.tp2.servidor.entity.Articulo;
import com.tp2.servidor.service.ArticuloService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articulos")
public class ArticuloController extends BaseRestController<Articulo, ArticuloRequest, String> {
    public ArticuloController(ArticuloService service) {
        super(service);
    }
}
