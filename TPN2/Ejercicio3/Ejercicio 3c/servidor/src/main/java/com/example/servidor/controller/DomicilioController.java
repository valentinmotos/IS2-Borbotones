package com.example.servidor.controller;

import com.example.servidor.model.Domicilio;
import com.example.servidor.service.DomicilioService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/domicilios")
public class DomicilioController extends BaseController<Domicilio> {
    public DomicilioController(DomicilioService service) {
        super(service);
    }
}
