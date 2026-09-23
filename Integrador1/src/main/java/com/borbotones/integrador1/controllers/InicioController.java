package com.borbotones.integrador1.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping({"/", "/inicio"})
    public String inicio(Model model) {
        return renderizar(model, "Zero | TP Integrador 1", "inicio");
    }

    @GetMapping("/productos")
    public String productos(Model model) {
        return renderizar(model, "Zero | Productos", "productos");
    }

    @GetMapping("/producto-detalle")
    public String detalleProducto(Model model) {
        return renderizar(model, "Zero | Detalle de producto", "producto-detalle");
    }

    @GetMapping("/carrito")
    public String carrito(Model model) {
        return renderizar(model, "Zero | Carrito", "carrito");
    }

    @GetMapping("/contacto")
    public String contacto(Model model) {
        return renderizar(model, "Zero | Contacto", "contacto");
    }

    @GetMapping("/demo/inicio-2")
    public String inicioAlternativoDos(Model model) {
        return renderizar(model, "Zero | Inicio alternativo 2", "inicio-02");
    }

    @GetMapping("/demo/inicio-3")
    public String inicioAlternativoTres(Model model) {
        return renderizar(model, "Zero | Inicio alternativo 3", "inicio-03");
    }

    private String renderizar(Model model, String titulo, String vista) {
        model.addAttribute("titulo", titulo);
        return vista;
    }
}
