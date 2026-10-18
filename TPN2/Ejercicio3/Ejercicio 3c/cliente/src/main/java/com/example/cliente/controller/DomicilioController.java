package com.example.cliente.controller;

import com.example.cliente.dto.DomicilioDTO;
import com.example.cliente.service.DomicilioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/domicilios")
public class DomicilioController extends BaseController<DomicilioDTO> {
    private final DomicilioService domicilioService;

    public DomicilioController(DomicilioService domicilioService) {
        super(domicilioService, "domicilios", "domicilios", "domicilio");
        this.domicilioService = domicilioService;
    }

    @Override
    protected void cargarDatosFormulario(Model model) {
        model.addAttribute("localidades", domicilioService.listarLocalidades());
    }
}
