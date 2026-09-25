package com.zero.ecommerce.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zero.ecommerce.dto.UsuarioActualDTO;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.services.UsuarioService;

@ControllerAdvice
public class UsuarioSesionAdvice {

    private final UsuarioService usuarioService;

    public UsuarioSesionAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute
    public void agregarDatosDeSesion(Model model) {
        Usuario usuario = usuarioService.usuarioActual().orElse(null);
        String nombreMostrar = usuarioService.nombreParaMostrar(usuario);
        UsuarioActualDTO usuarioActual = usuario == null ? null
                : new UsuarioActualDTO(usuario.getId(), usuario.getNombreUsuario(), nombreMostrar, usuario.getRol());
        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("nombreMostrar", nombreMostrar);
        model.addAttribute("rolActual", usuarioActual == null ? null : usuarioActual.rol());
    }
}
