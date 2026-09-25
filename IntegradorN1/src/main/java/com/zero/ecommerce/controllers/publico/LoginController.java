package com.zero.ecommerce.controllers.publico;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(name = "error", required = false) String error, Model model) {
        model.addAttribute("pageTitle", "Ingresar");
        model.addAttribute("error", mensajeError(error));
        return "publico/login";
    }

    private String mensajeError(String error) {
        if (error == null) {
            return null;
        }
        return switch (error) {
            case "eliminada" -> "La cuenta fue dada de baja.";
            case "activacion" -> "La cuenta todavía no está activada. Revisá tu correo para completar la activación.";
            case "deshabilitada" -> "La cuenta está deshabilitada. Contactá al administrador.";
            default -> "El correo o la contraseña son incorrectos.";
        };
    }
}
