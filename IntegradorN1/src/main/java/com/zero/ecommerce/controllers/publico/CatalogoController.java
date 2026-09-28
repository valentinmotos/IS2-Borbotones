package com.zero.ecommerce.controllers.publico;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.zero.ecommerce.dto.FiltroCatalogoDTO;
import com.zero.ecommerce.dto.ModeloCatalogoDTO;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CatalogoService;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.SubCategoriaService;

@Controller
public class CatalogoController {

    private static final int TAMANIO_CATALOGO = 16;
    private static final int TAMANIO_OFERTAS = 16;

    private final CatalogoService catalogoService;
    private final CategoriaService categoriaService;
    private final SubCategoriaService subCategoriaService;

    public CatalogoController(CatalogoService catalogoService, CategoriaService categoriaService,
            SubCategoriaService subCategoriaService) {
        this.catalogoService = catalogoService;
        this.categoriaService = categoriaService;
        this.subCategoriaService = subCategoriaService;
    }

    @GetMapping("/catalogo/{categoriaId}")
    public String porCategoria(@PathVariable String categoriaId,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String talle,
            @RequestParam(required = false) Boolean ofertas,
            @RequestParam(required = false) String orden,
            @RequestParam(name = "page", defaultValue = "1") int pagina, Model model) {
        Categoria categoria = buscarCategoria(categoriaId);
        String action = "/catalogo/" + categoriaId;
        prepararVista(model, categoria, null,
                catalogoService.buscarEnCategoria(categoriaId, null, null,
                        normalizarPrecio(precioMin), normalizarPrecio(precioMax), talle, ofertas, orden),
                pagina, construirBaseUrl(action, precioMin, precioMax, talle, ofertas, orden));
        prepararFiltros(model, action, precioMin, precioMax, talle, ofertas, orden);
        return "publico/catalogo";
    }

    @GetMapping("/catalogo/{categoriaId}/{subCategoriaId}")
    public String porSubCategoria(@PathVariable String categoriaId, @PathVariable String subCategoriaId,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String talle,
            @RequestParam(required = false) Boolean ofertas,
            @RequestParam(required = false) String orden,
            @RequestParam(name = "page", defaultValue = "1") int pagina, Model model) {
        Categoria categoria = buscarCategoria(categoriaId);
        SubCategoria subCategoria = buscarSubCategoria(subCategoriaId);
        if (subCategoria.getCategoria() == null || !categoriaId.equals(subCategoria.getCategoria().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La subcategoria no pertenece a la categoria.");
        }
        String action = "/catalogo/" + categoriaId + "/" + subCategoriaId;
        prepararVista(model, categoria, subCategoria,
                catalogoService.buscarEnCategoria(categoriaId, subCategoriaId, null,
                        normalizarPrecio(precioMin), normalizarPrecio(precioMax), talle, ofertas, orden),
                pagina, construirBaseUrl(action, precioMin, precioMax, talle, ofertas, orden));
        prepararFiltros(model, action, precioMin, precioMax, talle, ofertas, orden);
        return "publico/catalogo";
    }

    @GetMapping("/producto/{id}")
    public String detalle(@PathVariable String id, Model model) {
        try {
            ProductoCatalogoDTO producto = catalogoService.buscarDetalle(id);
            model.addAttribute("producto", producto);
            model.addAttribute("talles", catalogoService.listarTallesDelModelo(producto));
            model.addAttribute("relacionados", catalogoService.agruparPorModelo(
                    catalogoService.listarRelacionados(producto, 12)).stream().limit(4).toList());
            return "publico/producto-detalle";
        } catch (ErrorServiceException e) {
            // No se adjunta la excepcion de negocio: el handler global la convertiria en redireccion.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/ofertas")
    public String ofertas(@RequestParam(required = false) Double precioMinimo,
            @RequestParam(required = false) Double precioMaximo,
            @RequestParam(required = false) String talle,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        FiltroCatalogoDTO filtro = new FiltroCatalogoDTO(normalizarPrecio(precioMinimo),
                normalizarPrecio(precioMaximo), talle, orden);
        Page<ModeloCatalogoDTO> productos = catalogoService.listarOfertasPorModelo(filtro, page, TAMANIO_OFERTAS);
        model.addAttribute("productos", productos);
        model.addAttribute("paginaActual", productos.getNumber() + 1);
        model.addAttribute("totalPaginas", productos.getTotalPages());
        model.addAttribute("precioMinimo", filtro.precioMinimo());
        model.addAttribute("precioMaximo", filtro.precioMaximo());
        model.addAttribute("talle", talle);
        model.addAttribute("orden", filtro.ordenSeguro());
        model.addAttribute("talles", catalogoService.listarTallesDisponiblesEnOferta());
        model.addAttribute("baseUrl", construirBaseUrl(filtro));
        return "publico/ofertas";
    }

    private void prepararVista(Model model, Categoria categoria, SubCategoria subCategoria,
            List<ProductoCatalogoDTO> productos, int pagina, String baseUrl) {
        Page<ModeloCatalogoDTO> resultado = paginar(catalogoService.agruparPorModelo(productos), pagina);
        model.addAttribute("categoriaActual", categoria);
        model.addAttribute("subCategoriaActual", subCategoria);
        model.addAttribute("productos", resultado);
        model.addAttribute("paginaActual", resultado.getNumber() + 1);
        model.addAttribute("totalPaginas", resultado.getTotalPages());
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("pageTitle", subCategoria == null
                ? "Zero | " + categoria.getNombre()
                : "Zero | " + categoria.getNombre() + " - " + subCategoria.getNombre());
    }

    private <T> Page<T> paginar(List<T> productos, int pagina) {
        int totalPaginas = Math.max(1, (int) Math.ceil((double) productos.size() / TAMANIO_CATALOGO));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int desde = Math.min((actual - 1) * TAMANIO_CATALOGO, productos.size());
        int hasta = Math.min(desde + TAMANIO_CATALOGO, productos.size());
        return new PageImpl<>(productos.subList(desde, hasta), PageRequest.of(actual - 1, TAMANIO_CATALOGO),
                productos.size());
    }

    private Categoria buscarCategoria(String id) {
        try {
            return categoriaService.buscarCategoria(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private SubCategoria buscarSubCategoria(String id) {
        try {
            return subCategoriaService.buscarSubCategoria(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private Double normalizarPrecio(Double precio) {
        return precio != null && Double.isFinite(precio) && precio >= 0 ? precio : null;
    }

    private void prepararFiltros(Model model, String action, Double precioMin, Double precioMax,
            String talle, Boolean ofertas, String orden) {
        model.addAttribute("filtrosAction", action);
        model.addAttribute("filtrosLimpiarUrl", action);
        model.addAttribute("terminoBuscado", null);
        model.addAttribute("precioMin", normalizarPrecio(precioMin));
        model.addAttribute("precioMax", normalizarPrecio(precioMax));
        model.addAttribute("talleSeleccionado", talle);
        model.addAttribute("soloOfertas", Boolean.TRUE.equals(ofertas));
        model.addAttribute("ordenSeleccionado", orden);
    }

    private String construirBaseUrl(String action, Double precioMin, Double precioMax,
            String talle, Boolean ofertas, String orden) {
        StringBuilder url = new StringBuilder(action).append('?');
        agregar(url, "precioMin", normalizarPrecio(precioMin));
        agregar(url, "precioMax", normalizarPrecio(precioMax));
        agregar(url, "talle", talle);
        if (Boolean.TRUE.equals(ofertas)) {
            agregar(url, "ofertas", true);
        }
        agregar(url, "orden", orden);
        if (url.charAt(url.length() - 1) == '?' || url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }
        return url.toString();
    }

    private String construirBaseUrl(FiltroCatalogoDTO filtro) {
        StringBuilder url = new StringBuilder("/ofertas?");
        agregar(url, "precioMinimo", filtro.precioMinimo());
        agregar(url, "precioMaximo", filtro.precioMaximo());
        agregar(url, "talle", filtro.talle());
        agregar(url, "orden", filtro.ordenSeguro());
        if (url.charAt(url.length() - 1) == '?' || url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }
        return url.toString();
    }

    private void agregar(StringBuilder url, String nombre, Object valor) {
        if (valor != null && !valor.toString().isBlank()) {
            url.append(nombre).append('=').append(URLEncoder.encode(valor.toString(), StandardCharsets.UTF_8))
                    .append('&');
        }
    }
}
