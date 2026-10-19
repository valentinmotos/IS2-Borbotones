package com.example.servidor.controller;

import com.example.servidor.model.Prestamo;
import com.example.servidor.service.PrestamoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController extends BaseController<Prestamo> {
    public PrestamoController(PrestamoService service) {
        super(service);
    }
}
