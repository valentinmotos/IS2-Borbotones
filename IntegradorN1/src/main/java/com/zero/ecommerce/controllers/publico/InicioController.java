package com.zero.ecommerce.controllers.publico;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.services.CatalogoService;
import com.zero.ecommerce.services.CategoriaService;
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
    private final CategoriaService categoriaService;
    private final CatalogoService catalogoService;

    public InicioController(UsuarioService usuarioService, ClienteService clienteService,
            CategoriaService categoriaService, CatalogoService catalogoService) {
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.catalogoService = catalogoService;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        boolean perfilIncompleto = usuarioService.usuarioActual()
                .filter(usuario -> usuario.getRol() == RolUsuario.CLIENTE)
                .map(usuario -> !clienteService.perfilCompleto(usuario.getId()))
                .orElse(false);
        model.addAttribute("perfilIncompleto", perfilIncompleto);
        model.addAttribute("categoriasDestacadas", categoriaService.listarArbolActivo().stream().limit(4).toList());

        var visibles = catalogoService.listar(CatalogoFiltro.todos());
        model.addAttribute("productosOferta", catalogoService.listar(CatalogoFiltro.ofertas()).stream()
                .limit(8).toList());
        model.addAttribute("ultimosProductos", ultimos(visibles, 8));
        return "publico/inicio";
    }

    private java.util.List<ProductoCatalogoDTO> ultimos(java.util.List<ProductoCatalogoDTO> productos,
            int cantidad) {
        int desde = Math.max(0, productos.size() - cantidad);
        var ultimos = new java.util.ArrayList<>(productos.subList(desde, productos.size()));
        java.util.Collections.reverse(ultimos);
        return java.util.List.copyOf(ultimos);
    }
}
