package com.zero.ecommerce.controllers.publico;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "retorno", required = false) String retorno,
            @RequestParam(name = "idProducto", required = false) String idProducto,
            @RequestParam(name = "cantidad", defaultValue = "1") int cantidad,
            HttpServletRequest request,
            Model model) {
        if (idProducto != null && !idProducto.isBlank()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("pendienteIdProducto", idProducto);
            session.setAttribute("pendienteCantidad", cantidad);
            if (retorno != null && !retorno.isBlank()) {
                session.setAttribute("pendienteRetorno", retorno);
            }
        } else if (retorno != null && !retorno.isBlank()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("pendienteRetorno", retorno);
        }

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
