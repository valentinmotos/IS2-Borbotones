package com.example.servidor.controller;

import com.example.servidor.model.Libro;
import com.example.servidor.service.LibroService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/libros")
public class LibroController extends BaseController<Libro> {
    public LibroController(LibroService service) {
        super(service);
    }
}
