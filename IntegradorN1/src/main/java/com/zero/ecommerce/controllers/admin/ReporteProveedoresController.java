package com.zero.ecommerce.controllers.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.ReporteProveedoresService;

/** Reporte de proveedores (E5-05): el proveedor más económico de cada producto y la comparación entre proveedores. */
@Controller
@RequestMapping("/admin/reportes/proveedores")
public class ReporteProveedoresController {

    private final ReporteProveedoresService service;
    private final CategoriaService categoriaService;

    public ReporteProveedoresController(ReporteProveedoresService service, CategoriaService categoriaService) {
        this.service = service;
        this.categoriaService = categoriaService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "reportes-proveedores";
    }

    @GetMapping
    public String reporte(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "") String categoria, Model model) {
        model.addAttribute("pageTitle", "Reporte de proveedores");
        model.addAttribute("reporte", service.generar(buscar, categoria));
        model.addAttribute("opcionesCategoria", opcionesCategoria());
        model.addAttribute("buscar", buscar);
        model.addAttribute("categoria", categoria);
        return "admin/reportes/proveedores";
    }

    private Map<String, String> opcionesCategoria() {
        Map<String, String> opciones = new LinkedHashMap<>();
        for (Categoria categoria : categoriaService.listarCategoriaActiva()) {
            opciones.put(categoria.getId(), categoria.getNombre());
        }
        return opciones;
    }

    @GetMapping("/exportar")
    public org.springframework.http.ResponseEntity<byte[]> exportarCsv(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "") String categoria) {

        byte[] contenido = service.exportarCsv(buscar, categoria);
        String filename = "reporte_proveedores.csv";

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }
}
