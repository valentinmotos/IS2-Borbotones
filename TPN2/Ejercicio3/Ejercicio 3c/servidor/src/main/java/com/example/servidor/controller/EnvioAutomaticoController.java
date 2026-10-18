package com.example.servidor.controller;

import com.example.servidor.model.EnvioAutomatico;
import com.example.servidor.service.EnvioAutomaticoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/envios-automaticos")
public class EnvioAutomaticoController extends BaseController<EnvioAutomatico> {
    public EnvioAutomaticoController(EnvioAutomaticoService service) {
        super(service);
    }
}
