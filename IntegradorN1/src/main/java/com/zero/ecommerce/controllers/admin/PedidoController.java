package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.zero.ecommerce.dto.PedidoDTO;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.OrdenCompraService;

/** Panel de pedidos (E4-06): listado con filtros y tarjetas por estado, y detalle para preparar el envío. */
@Controller
@RequestMapping("/admin/pedidos")
public class PedidoController {

    private final OrdenCompraService service;
    private final FormaDePagoService formaDePagoService;

    public PedidoController(OrdenCompraService service, FormaDePagoService formaDePagoService) {
        this.service = service;
        this.formaDePagoService = formaDePagoService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "pedidos";
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String formaPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "") String cliente,
            Model model) {
        List<PedidoDTO> pedidos;
        try {
            pedidos = service.listarPedido(service.convertirEstado(estado), formaPago, desde, hasta, cliente);
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            pedidos = List.of();
        }
        model.addAttribute("pageTitle", "Pedidos");
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("totalPedidos", pedidos.size());
        model.addAttribute("cantidadesPorEstado", service.contarPedidoPorEstado());
        model.addAttribute("opcionesEstado", service.listarEstadoPedido());
        model.addAttribute("opcionesFormaPago", opcionesFormaPago());
        model.addAttribute("estado", estado);
        model.addAttribute("formaPago", formaPago);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("cliente", cliente);
        return "admin/pedidos/listado";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model) {
        OrdenCompra pedido = buscarO404(id);
        model.addAttribute("pageTitle", "Pedido " + pedido.getIdentificadorCompra());
        model.addAttribute("pedido", pedido);
        model.addAttribute("factura", service.buscarFacturaDePedido(id).orElse(null));
        model.addAttribute("pasos", pedido.pasosSeguimiento());
        return "admin/pedidos/detalle";
    }

    private Map<String, String> opcionesFormaPago() {
        Map<String, String> opciones = new LinkedHashMap<>();
        for (FormaDePago formaDePago : formaDePagoService.listarFormaDePagoActivo()) {
            opciones.put(formaDePago.getId(),
                    formaDePago.getObservacion() + " (" + formaDePago.getTipoPago().getDescripcion() + ")");
        }
        return opciones;
    }

    private OrdenCompra buscarO404(String id) {
        try {
            return service.buscarPedido(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
