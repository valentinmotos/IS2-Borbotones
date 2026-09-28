package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.ReporteProveedoresService;

/** Reporte de proveedores (E5-05): el proveedor más económico de cada producto y la comparación entre proveedores. */
@Controller
@RequestMapping("/admin/reportes/proveedores")
public class ReporteProveedoresController {

    private final ReporteProveedoresService service;
    private final CategoriaService categoriaService;
    private final EmpresaService empresaService;

    public ReporteProveedoresController(ReporteProveedoresService service, CategoriaService categoriaService,
            EmpresaService empresaService) {
        this.service = service;
        this.categoriaService = categoriaService;
        this.empresaService = empresaService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "reportes-proveedores";
    }

    @GetMapping
    public String reporte(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "") String categoria, Model model) throws ErrorServiceException {
        model.addAttribute("pageTitle", "Reporte de proveedores");
        model.addAttribute("empresa", empresaService.buscarSedeCentral());
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
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "") String categoria) {
        byte[] contenido = service.exportarCsv(buscar, categoria);
        String nombreArchivo = "reporte_proveedores_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }
}
