package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            Model model, RedirectAttributes redirectAttributes) {

        // Valores por defecto: primer y último día del mes actual
        LocalDate desdeEfectivo = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate hastaEfectivo = hasta != null ? hasta : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        try {
            model.addAttribute("pageTitle", "Reporte de ventas");
            model.addAttribute("reporte", service.generar(desdeEfectivo, hastaEfectivo));
            model.addAttribute("desde", desdeEfectivo);
            model.addAttribute("hasta", hastaEfectivo);
        } catch (ErrorServiceException e) {
            model.addAttribute("pageTitle", "Reporte de ventas");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reporte", null);
            model.addAttribute("desde", desdeEfectivo);
            model.addAttribute("hasta", hastaEfectivo);
        }

        return "admin/reportes/ventas";
    }
}
