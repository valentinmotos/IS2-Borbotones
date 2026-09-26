package com.zero.ecommerce.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zero.ecommerce.dto.UsuarioActualDTO;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.services.CarritoService;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class UsuarioSesionAdvice {

    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final CarritoService carritoService;

    public UsuarioSesionAdvice(UsuarioService usuarioService, ClienteService clienteService,
            CarritoService carritoService) {
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
        this.carritoService = carritoService;
    }

    @ModelAttribute
    public void agregarDatosDeSesion(Model model, HttpServletRequest request) {
        Usuario usuario = usuarioService.usuarioActual().orElse(null);
        String nombreMostrar = usuarioService.nombreParaMostrar(usuario);
        UsuarioActualDTO usuarioActual = usuario == null ? null
                : new UsuarioActualDTO(usuario.getId(), usuario.getNombreUsuario(), nombreMostrar, usuario.getRol());
        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("nombreMostrar", nombreMostrar);
        model.addAttribute("rolActual", usuarioActual == null ? null : usuarioActual.rol());

        // E3-08: expone la cantidad de ítems del carrito para el cliente logueado
        int cantidadCarrito = 0;
        if (usuario != null && usuario.getRol() == RolUsuario.CLIENTE) {
            cantidadCarrito = clienteService.buscarClientePorUsuario(usuario.getId())
                    .map(c -> carritoService.contarItemsCarrito(c.getId()))
                    .orElse(0);
        }
        model.addAttribute("cantidadCarrito", cantidadCarrito);

        // Mensajes flash manuales guardados en sesión (ej. tras login con retorno)
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object msgExito = session.getAttribute("mensajeExitoFlash");
            if (msgExito != null && !model.containsAttribute("exito")) {
                model.addAttribute("exito", msgExito);
                session.removeAttribute("mensajeExitoFlash");
            }
            Object msgError = session.getAttribute("mensajeErrorFlash");
            if (msgError != null && !model.containsAttribute("error")) {
                model.addAttribute("error", msgError);
                session.removeAttribute("mensajeErrorFlash");
            }
        }
    }
}

