package com.example.cliente.controller;

import com.example.cliente.dto.LibroDTO;
import com.example.cliente.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/libros")
public class LibroController extends BaseController<LibroDTO> {
    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        super(libroService, "libros", "libros", "libro");
        this.libroService = libroService;
    }

    @Override
    protected void cargarDatosFormulario(Model model) {
        model.addAttribute("personas", libroService.listarPersonas());
    }
}
