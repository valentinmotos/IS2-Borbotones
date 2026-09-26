package com.zero.ecommerce.controllers.publico;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.zero.ecommerce.dto.FiltroCatalogoDTO;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CatalogoService;

@Controller
public class CatalogoController {

    private static final int PRODUCTOS_POR_PAGINA = 8;

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
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
            // Sin adjuntar la excepcion de negocio como causa: el handler global la convertiria
            // en una redireccion, cuando esta ruta debe responder realmente con 404.
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
        var productos = catalogoService.listarOfertas(filtro, page, PRODUCTOS_POR_PAGINA);
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
