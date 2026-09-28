package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.ReporteVentasService;

/** Reporte de ventas (E5-01, RF28): detalle de facturas pagadas con filtro de rango de fechas. */
@Controller
@RequestMapping("/admin/reportes/ventas")
public class ReporteVentasController {

    private final ReporteVentasService service;
    private final EmpresaService empresaService;

    public ReporteVentasController(ReporteVentasService service, EmpresaService empresaService) {
        this.service = service;
        this.empresaService = empresaService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "reportes-ventas";
    }

    @GetMapping
    public String reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        // Valores por defecto: primer y último día del mes actual.
        LocalDate hoy = LocalDate.now();
        LocalDate desdeEfectivo = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate hastaEfectivo = hasta != null ? hasta : hoy.withDayOfMonth(hoy.lengthOfMonth());

        model.addAttribute("pageTitle", "Reporte de ventas");
        model.addAttribute("desde", desdeEfectivo);
        model.addAttribute("hasta", hastaEfectivo);

        try {
            model.addAttribute("empresa", empresaService.buscarSedeCentral());
            model.addAttribute("reporte", service.generar(desdeEfectivo, hastaEfectivo));
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reporte", null);
        }

        return "admin/reportes/ventas";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws ErrorServiceException {

        LocalDate hoy = LocalDate.now();
        LocalDate desdeEfectivo = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate hastaEfectivo = hasta != null ? hasta : hoy.withDayOfMonth(hoy.lengthOfMonth());

        byte[] contenido = service.exportarCsv(desdeEfectivo, hastaEfectivo);
        String nombreArchivo = "reporte_ventas_" + desdeEfectivo + "_a_" + hastaEfectivo + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }
}
