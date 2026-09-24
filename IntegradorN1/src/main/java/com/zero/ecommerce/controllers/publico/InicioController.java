package com.zero.ecommerce.controllers.publico;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página mínima para verificar que la app levanta. E0-02 la reemplaza por la home con el template.
 */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio() {
        return "publico/inicio";
    }
}
