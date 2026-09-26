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
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CatalogoService;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.SubCategoriaService;

@Controller
public class CatalogoController {

    private static final int TAMANIO_CATALOGO = 4;
    private static final int TAMANIO_OFERTAS = 8;

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
            @RequestParam(name = "page", defaultValue = "1") int pagina, Model model) {
        Categoria categoria = buscarCategoria(categoriaId);
        prepararVista(model, categoria, null, catalogoService.listarPorCategoria(categoriaId), pagina,
                "/catalogo/" + categoriaId);
        return "publico/catalogo";
    }

    @GetMapping("/catalogo/{categoriaId}/{subCategoriaId}")
    public String porSubCategoria(@PathVariable String categoriaId, @PathVariable String subCategoriaId,
            @RequestParam(name = "page", defaultValue = "1") int pagina, Model model) {
        Categoria categoria = buscarCategoria(categoriaId);
        SubCategoria subCategoria = buscarSubCategoria(subCategoriaId);
        if (subCategoria.getCategoria() == null || !categoriaId.equals(subCategoria.getCategoria().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La subcategoria no pertenece a la categoria.");
        }
        prepararVista(model, categoria, subCategoria,
                catalogoService.listarPorSubCategoria(categoriaId, subCategoriaId), pagina,
                "/catalogo/" + categoriaId + "/" + subCategoriaId);
        return "publico/catalogo";
    }

    @GetMapping("/producto/{id}")
    public String detalle(@PathVariable String id, Model model) {
        try {
            ProductoCatalogoDTO producto = catalogoService.buscarDetalle(id);
            model.addAttribute("producto", producto);
            model.addAttribute("talles", catalogoService.listarTallesDelModelo(producto));
            model.addAttribute("relacionados", catalogoService.listarRelacionados(producto, 4));
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
        Page<ProductoCatalogoDTO> productos = catalogoService.listarOfertas(filtro, page, TAMANIO_OFERTAS);
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
        Page<ProductoCatalogoDTO> resultado = paginar(productos, pagina);
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

    private Page<ProductoCatalogoDTO> paginar(List<ProductoCatalogoDTO> productos, int pagina) {
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
