package com.zero.ecommerce.controllers.cliente;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.UsuarioService;

/** Página "Mi cuenta" del cliente logueado, con accesos a perfil, clave y compras (E2-08). */
@Controller
public class CuentaClienteController {

    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public CuentaClienteController(ClienteService clienteService, UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/cliente")
    public String cuenta(Model model) {
        // /cliente/** exige el rol CLIENTE, así que siempre hay un usuario logueado.
        Usuario usuario = usuarioService.usuarioActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        model.addAttribute("pageTitle", "Mi cuenta");
        model.addAttribute("perfilCompleto", clienteService.perfilCompleto(usuario.getId()));
        return "cliente/cuenta";
    }
}
