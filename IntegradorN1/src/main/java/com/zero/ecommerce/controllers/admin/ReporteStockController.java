package com.zero.ecommerce.controllers.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zero.ecommerce.dto.ProductoStockDTO;
import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.entities.enums.EstadoStock;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.ReporteStockService;

@Controller
@RequestMapping("/admin/reportes/stock")
public class ReporteStockController {

    private final ReporteStockService reporteStockService;
    private final CategoriaService categoriaService;

    public ReporteStockController(ReporteStockService reporteStockService, CategoriaService categoriaService) {
        this.reporteStockService = reporteStockService;
        this.categoriaService = categoriaService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "reporte-stock";
    }

    @GetMapping
    public String mostrar(@RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String categoria, Model model) throws ErrorServiceException {
        EstadoStock estadoSeleccionado = convertirEstado(estado);
        ReporteStockDTO reporte = reporteStockService.generar();
        List<ProductoStockDTO> productos = reporte.productos().stream()
                .filter(producto -> estadoSeleccionado == null || producto.estado() == estadoSeleccionado)
                .filter(producto -> categoria.isBlank() || producto.categoriaId().equals(categoria))
                .toList();

        model.addAttribute("pageTitle", "Reporte de productos y stock");
        model.addAttribute("reporte", reporte);
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categoriaService.listarCategoriaActiva());
        model.addAttribute("estado", estadoSeleccionado == null ? "" : estadoSeleccionado.name());
        model.addAttribute("categoria", categoria);
        return "admin/reportes/stock";
    }

    private EstadoStock convertirEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoStock.valueOf(estado.strip().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
