package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zero.ecommerce.dto.ProductoStockDTO;
import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.entities.enums.EstadoStock;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.ProveedorService;
import com.zero.ecommerce.services.ReposicionStockService;
import com.zero.ecommerce.services.ReporteStockService;

@Controller
@RequestMapping("/admin/reportes/stock")
public class ReporteStockController {

    private final ReporteStockService reporteStockService;
    private final CategoriaService categoriaService;
    private final ReposicionStockService reposicionStockService;
    private final ProveedorService proveedorService;
    private final EmpresaService empresaService;

    public ReporteStockController(ReporteStockService reporteStockService, CategoriaService categoriaService,
            ReposicionStockService reposicionStockService, ProveedorService proveedorService,
            EmpresaService empresaService) {
        this.reporteStockService = reporteStockService;
        this.categoriaService = categoriaService;
        this.reposicionStockService = reposicionStockService;
        this.proveedorService = proveedorService;
        this.empresaService = empresaService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "reporte-stock";
    }

    @GetMapping
    public String mostrar(@RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String categoria, Model model) throws ErrorServiceException {
        EstadoStock estadoSeleccionado = reporteStockService.convertirEstado(estado);
        ReporteStockDTO reporte = reporteStockService.generar();
        List<ProductoStockDTO> productos = reporteStockService.filtrar(reporte, estadoSeleccionado, categoria);

        model.addAttribute("pageTitle", "Reporte de productos y stock");
        model.addAttribute("empresa", empresaService.buscarSedeCentral());
        model.addAttribute("reporte", reporte);
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categoriaService.listarCategoriaActiva());
        model.addAttribute("reposiciones", reposicionStockService.listar(reporte.productos()));
        model.addAttribute("proveedores", proveedorService.listarProveedorActivo());
        model.addAttribute("estado", estadoSeleccionado == null ? "" : estadoSeleccionado.name());
        model.addAttribute("categoria", categoria);
        return "admin/reportes/stock";
    }

    @GetMapping("/reposicion/{idProducto}/whatsapp")
    public String pedirPorWhatsApp(@PathVariable String idProducto,
            @RequestParam(required = false) String proveedor) throws ErrorServiceException {
        return "redirect:" + reposicionStockService.generarUrlWhatsApp(idProducto, proveedor);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String categoria) throws ErrorServiceException {
        byte[] contenido = reporteStockService.exportarCsv(estado, categoria);
        String nombreArchivo = "reporte_stock_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }
}
