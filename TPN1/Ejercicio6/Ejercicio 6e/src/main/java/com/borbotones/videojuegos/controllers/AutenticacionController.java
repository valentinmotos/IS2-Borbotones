package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.dto.RegistroForm;
import com.borbotones.videojuegos.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Objects;

@Controller
public class AutenticacionController {
    private final UsuarioService usuarioService;

    public AutenticacionController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registro(@ModelAttribute("registro") RegistroForm registro) {
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @Valid @ModelAttribute("registro") RegistroForm registro,
            BindingResult result
    ) {
        if (!Objects.equals(registro.getPassword(), registro.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "password.mismatch", "Las contrasenas no coinciden");
        }
        if (result.hasErrors()) {
            return "auth/registro";
        }

        try {
            usuarioService.registrar(registro);
            return "redirect:/login?registro";
        } catch (IllegalArgumentException ex) {
            result.reject("registro.invalido", ex.getMessage());
            return "auth/registro";
        }
    }
}
