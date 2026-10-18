package com.example.cliente.controller;

import com.example.cliente.dto.EnvioAutomaticoDTO;
import com.example.cliente.service.EnvioAutomaticoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/envios-automaticos")
public class EnvioAutomaticoController extends BaseController<EnvioAutomaticoDTO> {
    public EnvioAutomaticoController(EnvioAutomaticoService envioAutomaticoService) {
        super(envioAutomaticoService, "envios-automaticos", "envios", "envio");
    }
}
