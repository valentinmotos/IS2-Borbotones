package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zero.ecommerce.dto.DashboardDTO;
import com.zero.ecommerce.dto.PedidoDTO;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.DashboardService;
import com.zero.ecommerce.services.OrdenCompraService;

@Controller
public class AdminInicioController {

    private final DashboardService dashboardService;
    private final OrdenCompraService ordenCompraService;

    public AdminInicioController(DashboardService dashboardService, OrdenCompraService ordenCompraService) {
        this.dashboardService = dashboardService;
        this.ordenCompraService = ordenCompraService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "inicio";
    }

    @GetMapping("/admin")
    public String inicio(Model model) throws ErrorServiceException {
        DashboardDTO dashboard = dashboardService.generar();
        List<PedidoDTO> pedidosRecientes = ordenCompraService.listarPedido(null, null, null, null, null)
                .stream()
                .limit(10)
                .toList();

        model.addAttribute("pageTitle", "Inicio");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("pedidosRecientes", pedidosRecientes);
        model.addAttribute("chartLabels", dashboard.ventasUltimosSeisMeses().stream().map(v -> v.etiqueta()).toList());
        model.addAttribute("chartValues", dashboard.ventasUltimosSeisMeses().stream().map(v -> v.monto()).toList());
        LocalDate hoy = LocalDate.now();
        model.addAttribute("inicioMes", hoy.withDayOfMonth(1));
        model.addAttribute("finMes", hoy.withDayOfMonth(hoy.lengthOfMonth()));
        return "admin/inicio";
    }
}
