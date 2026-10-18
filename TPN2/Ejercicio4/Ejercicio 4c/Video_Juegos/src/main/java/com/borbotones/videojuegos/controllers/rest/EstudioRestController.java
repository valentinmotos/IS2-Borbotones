package com.borbotones.videojuegos.controllers.rest;

import com.borbotones.videojuegos.controllers.BaseRestControllerTemplate;
import com.borbotones.videojuegos.entities.Estudio;
import com.borbotones.videojuegos.services.EstudioService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estudios")
public class EstudioRestController extends BaseRestControllerTemplate<Estudio> {
    public EstudioRestController(EstudioService service) {
        super(service);
    }
}
