package com.example.cliente.controller;

import com.example.cliente.dto.PrestamoDTO;
import com.example.cliente.service.PrestamoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/prestamos")
public class PrestamoController extends BaseController<PrestamoDTO> {
    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        super(prestamoService, "prestamos", "prestamos", "prestamo");
        this.prestamoService = prestamoService;
    }

    @Override
    protected void cargarDatosFormulario(Model model) {
        model.addAttribute("libros", prestamoService.listarLibros());
        model.addAttribute("personas", prestamoService.listarPersonas());
    }
}
