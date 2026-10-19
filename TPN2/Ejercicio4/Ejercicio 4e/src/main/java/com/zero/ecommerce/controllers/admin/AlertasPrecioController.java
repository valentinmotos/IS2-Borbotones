package com.zero.ecommerce.controllers.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.zero.ecommerce.dto.ProductoPrecioVencidoDTO;
import com.zero.ecommerce.services.VigenciaPrecioService;

@Controller
@RequestMapping("/admin/precios/alertas")
public class AlertasPrecioController {

    private final VigenciaPrecioService vigenciaPrecioService;

    public AlertasPrecioController(VigenciaPrecioService vigenciaPrecioService) {
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "precios-alertas";
    }

    @GetMapping
    public String listado(Model model) {
        List<ProductoPrecioVencidoDTO> alertas = vigenciaPrecioService.listarProductosConPrecioVencido();
        Map<String, List<ProductoPrecioVencidoDTO>> alertasPorCategoria = alertas.stream()
                .collect(Collectors.groupingBy(ProductoPrecioVencidoDTO::categoriaNombre,
                        LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("pageTitle", "Alertas de precios");
        model.addAttribute("alertas", alertas);
        model.addAttribute("alertasPorCategoria", alertasPorCategoria);
        return "admin/precios/alertas";
    }
}
