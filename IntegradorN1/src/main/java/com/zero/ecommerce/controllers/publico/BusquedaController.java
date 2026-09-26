package com.zero.ecommerce.controllers.publico;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.services.CatalogoService;

@Controller
public class BusquedaController {

    private static final int TAMANIO_PAGINA = 12;

    private final CatalogoService catalogoService;

    public BusquedaController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @RequestParam(name = "precioMin", required = false) Double precioMin,
            @RequestParam(name = "precioMax", required = false) Double precioMax,
            @RequestParam(name = "talle", required = false) String talle,
            @RequestParam(name = "ofertas", required = false) Boolean ofertas,
            @RequestParam(name = "orden", required = false, defaultValue = "") String orden,
            @RequestParam(name = "page", defaultValue = "1") int pagina,
            Model model) {

        CatalogoFiltro filtro = CatalogoFiltro.busqueda(precioMin, precioMax, talle, ofertas, orden);
        List<ProductoCatalogoDTO> todos = catalogoService.buscar(q, filtro);

        Page<ProductoCatalogoDTO> resultado = paginar(todos, pagina);
        String baseUrl = construirBaseUrl(q, precioMin, precioMax, talle, ofertas, orden);

        model.addAttribute("productos", resultado);
        model.addAttribute("paginaActual", resultado.getNumber() + 1);
        model.addAttribute("totalPaginas", resultado.getTotalPages());
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("terminoBuscado", q);
        model.addAttribute("precioMin", precioMin);
        model.addAttribute("precioMax", precioMax);
        model.addAttribute("talleSeleccionado", talle);
        model.addAttribute("soloOfertas", Boolean.TRUE.equals(ofertas));
        model.addAttribute("ordenSeleccionado", orden);
        model.addAttribute("pageTitle", q.isBlank() ? "Zero | Buscar" : "Zero | Resultados: " + q);

        return "publico/buscar";
    }

    private Page<ProductoCatalogoDTO> paginar(List<ProductoCatalogoDTO> productos, int pagina) {
        int totalPaginas = Math.max(1, (int) Math.ceil((double) productos.size() / TAMANIO_PAGINA));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int desde = Math.min((actual - 1) * TAMANIO_PAGINA, productos.size());
        int hasta = Math.min(desde + TAMANIO_PAGINA, productos.size());
        return new PageImpl<>(
                productos.subList(desde, hasta),
                PageRequest.of(actual - 1, TAMANIO_PAGINA),
                productos.size());
    }

    private String construirBaseUrl(String q, Double precioMin, Double precioMax,
            String talle, Boolean ofertas, String orden) {
        StringBuilder sb = new StringBuilder("/buscar?q=");
        sb.append(encode(q));
        if (precioMin != null) {
            sb.append("&precioMin=").append(precioMin.intValue());
        }
        if (precioMax != null) {
            sb.append("&precioMax=").append(precioMax.intValue());
        }
        if (talle != null && !talle.isBlank()) {
            sb.append("&talle=").append(encode(talle));
        }
        if (Boolean.TRUE.equals(ofertas)) {
            sb.append("&ofertas=true");
        }
        if (orden != null && !orden.isBlank()) {
            sb.append("&orden=").append(encode(orden));
        }
        return sb.toString();
    }

    private String encode(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(valor, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return valor;
        }
    }
}
