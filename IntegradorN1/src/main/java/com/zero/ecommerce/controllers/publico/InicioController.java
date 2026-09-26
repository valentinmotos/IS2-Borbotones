package com.zero.ecommerce.controllers.publico;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.UsuarioService;

/**
 * Página mínima para verificar que la app levanta. E0-02 la reemplaza por la home con el template.
 * Muestra el aviso de perfil incompleto a los clientes logueados (E2-08).
 */
@Controller
public class InicioController {

    private final UsuarioService usuarioService;
    private final ClienteService clienteService;

    public InicioController(UsuarioService usuarioService, ClienteService clienteService) {
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        boolean perfilIncompleto = usuarioService.usuarioActual()
                .filter(usuario -> usuario.getRol() == RolUsuario.CLIENTE)
                .map(usuario -> !clienteService.perfilCompleto(usuario.getId()))
                .orElse(false);
        model.addAttribute("perfilIncompleto", perfilIncompleto);
        return "publico/inicio";
    }
}
