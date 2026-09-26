package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.PedidoDTO;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.PedidoAccionService;

/** Panel de pedidos (E4-06 / E4-07): consulta, detalle y acciones sobre el circuito de entrega. */
@Controller
@RequestMapping("/admin/pedidos")
public class PedidoController {

    private final OrdenCompraService service;
    private final FormaDePagoService formaDePagoService;
    private final PedidoAccionService accionService;

    public PedidoController(OrdenCompraService service, FormaDePagoService formaDePagoService,
            PedidoAccionService accionService) {
        this.service = service;
        this.formaDePagoService = formaDePagoService;
        this.accionService = accionService;
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

    @PostMapping("/{id}/confirmar-pago")
    public String confirmarPago(@PathVariable String id, Authentication authentication, RedirectAttributes flash) {
        try {
            OrdenCompra pedido = accionService.confirmarPago(id, authentication.getName());
            flash.addFlashAttribute("exito", "Se confirmó el pago del pedido "
                    + pedido.getIdentificadorCompra() + ".");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigirDetalle(id);
    }

    @PostMapping("/{id}/marcar-enviado")
    public String marcarEnviado(@PathVariable String id, Authentication authentication, RedirectAttributes flash) {
        try {
            OrdenCompra pedido = accionService.marcarEnviado(id, authentication.getName());
            flash.addFlashAttribute("exito", "El pedido " + pedido.getIdentificadorCompra()
                    + " fue marcado como enviado.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigirDetalle(id);
    }

    @PostMapping("/{id}/marcar-entregado")
    public String marcarEntregado(@PathVariable String id, Authentication authentication, RedirectAttributes flash) {
        try {
            OrdenCompra pedido = accionService.marcarEntregado(id, authentication.getName());
            flash.addFlashAttribute("exito", "El pedido " + pedido.getIdentificadorCompra()
                    + " fue marcado como entregado.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirigirDetalle(id);
    }

    @PostMapping("/{id}/anular")
    public String anular(@PathVariable String id, @RequestParam(defaultValue = "") String motivo,
            Authentication authentication, RedirectAttributes flash) {
        try {
            OrdenCompra pedido = accionService.anular(id, authentication.getName(), motivo);
            flash.addFlashAttribute("exito", "El pedido " + pedido.getIdentificadorCompra() + " fue anulado.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("motivoAnulacion", motivo);
        }
        return redirigirDetalle(id);
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

    private String redirigirDetalle(String id) {
        return "redirect:/admin/pedidos/" + id;
    }
}
