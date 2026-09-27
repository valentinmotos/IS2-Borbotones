package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ReporteVentasService;

/** Reporte de ventas (E5-01, RF28): detalle de facturas pagadas con filtro de rango de fechas. */
@Controller
@RequestMapping("/admin/reportes/ventas")
public class ReporteVentasController {

    private final ReporteVentasService service;

    public ReporteVentasController(ReporteVentasService service) {
        this.service = service;
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
            model.addAttribute("reporte", service.generar(desdeEfectivo, hastaEfectivo));
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reporte", null);
        }

        return "admin/reportes/ventas";
    }

    @GetMapping("/exportar")
    public org.springframework.http.ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws ErrorServiceException {

        LocalDate hoy = LocalDate.now();
        LocalDate desdeEfectivo = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate hastaEfectivo = hasta != null ? hasta : hoy.withDayOfMonth(hoy.lengthOfMonth());

        byte[] contenido = service.exportarCsv(desdeEfectivo, hastaEfectivo);
        String filename = "reporte_ventas_" + desdeEfectivo + "_a_" + hastaEfectivo + ".csv";

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }
}
