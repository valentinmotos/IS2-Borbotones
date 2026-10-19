package com.zero.ecommerce.controllers.publico;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.CategoriaArbolDTO;
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
        var categoriasDestacadas = categoriaService.listarArbolActivo().stream().limit(4).toList();
        model.addAttribute("categoriasDestacadas", categoriasDestacadas);

        var visibles = catalogoService.listar(CatalogoFiltro.todos());
        model.addAttribute("productosOferta", catalogoService.agruparPorModelo(
                catalogoService.listar(CatalogoFiltro.ofertas())).stream().limit(4).toList());
        model.addAttribute("ultimosProductos", catalogoService.agruparPorModelo(
                ultimos(visibles, visibles.size())).stream().limit(8).toList());
        model.addAttribute("imagenesCategoria", imagenesPorCategoria(categoriasDestacadas, visibles));
        return "publico/inicio";
    }

    private java.util.Map<String, String> imagenesPorCategoria(java.util.List<CategoriaArbolDTO> categorias,
            java.util.List<ProductoCatalogoDTO> productos) {
        java.util.Map<String, String> imagenes = new java.util.HashMap<>();
        for (CategoriaArbolDTO categoria : categorias) {
            categoria.subCategorias().stream()
                    .sorted(java.util.Comparator.comparing(subCategoria -> !"Ropa".equals(subCategoria.getNombre())))
                    .flatMap(subCategoria -> productos.stream()
                            .filter(producto -> subCategoria.getId().equals(producto.subCategoriaId())))
                    .filter(producto -> producto.imagenId() != null)
                    .findFirst()
                    .ifPresent(producto -> imagenes.put(categoria.id(), producto.imagenId()));
        }
        return imagenes;
    }

    private java.util.List<ProductoCatalogoDTO> ultimos(java.util.List<ProductoCatalogoDTO> productos,
            int cantidad) {
        int desde = Math.max(0, productos.size() - cantidad);
        var ultimos = new java.util.ArrayList<>(productos.subList(desde, productos.size()));
        java.util.Collections.reverse(ultimos);
        return java.util.List.copyOf(ultimos);
    }
}
