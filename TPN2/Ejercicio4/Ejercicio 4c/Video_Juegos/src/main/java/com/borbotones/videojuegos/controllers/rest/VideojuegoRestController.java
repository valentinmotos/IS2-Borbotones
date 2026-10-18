package com.borbotones.videojuegos.controllers.rest;

import com.borbotones.videojuegos.controllers.BaseRestControllerTemplate;
import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.services.VideojuegoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videojuegos")
public class VideojuegoRestController extends BaseRestControllerTemplate<Videojuego> {
    public VideojuegoRestController(VideojuegoService service) {
        super(service);
    }
}
