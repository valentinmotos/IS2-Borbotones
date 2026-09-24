package com.zero.ecommerce.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DevController {

    @GetMapping("/dev/ejemplo-publico")
    public String ejemploPublico() {
        return "dev/ejemplo-publico";
    }

    @GetMapping("/dev/ejemplo-admin")
    public String ejemploAdmin() {
        return "dev/ejemplo-admin";
    }

    @GetMapping("/dev/componentes")
    public String componentes(Model model) {
        model.addAttribute("exito", "La operación se realizó correctamente.");
        model.addAttribute("error", "Este es un mensaje de validación de ejemplo.");
        model.addAttribute("encabezados", List.of("Producto", "Estado", "Precio"));
        model.addAttribute("filas", List.of(
            List.of("Remera Zero Pro", "Publicado", "$12.500"),
            List.of("Short Zero Move", "Pendiente", "$9.900")));
        model.addAttribute("filasVacias", List.of());
        return "dev/componentes";
    }
}
