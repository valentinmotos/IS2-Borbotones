package com.zero.ecommerce.controllers.cliente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.UsuarioService;
import com.zero.ecommerce.services.VentaService;

/**
 * "Mis compras" del cliente logueado (E4-04): historial (RF21), detalle con seguimiento (RF22 y RF23) y anulación
 * (RF19). Un cliente no puede ver ni anular compras de otro: devuelve 403.
 */
@Controller
@RequestMapping("/cliente/compras")
public class CompraClienteController {

    private static final String BASE = "/cliente/compras";

    private final OrdenCompraService ordenCompraService;
    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public CompraClienteController(OrdenCompraService ordenCompraService, VentaService ventaService,
            ClienteService clienteService, UsuarioService usuarioService) {
        this.ordenCompraService = ordenCompraService;
        this.ventaService = ventaService;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        // Un cliente que todavía no cargó su perfil no tiene compras.
        Cliente cliente = clienteActual();
        model.addAttribute("pageTitle", "Mis compras");
        model.addAttribute("compras",
                cliente == null ? List.of() : ordenCompraService.listarFilaCompraCliente(cliente.getId()));
        return "cliente/compras/listado";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model) {
        OrdenCompra compra = buscarCompraPropia(id);
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(id).orElse(null);
        model.addAttribute("pageTitle", "Compra " + compra.getIdentificadorCompra());
        model.addAttribute("compra", compra);
        model.addAttribute("factura", factura);
        model.addAttribute("pasos", compra.pasosSeguimiento());
        model.addAttribute("puedeAnularse", compra.puedeAnularse(false));
        model.addAttribute("pagarConMercadoPago", compra.getEstadoOrdenCompra() == EstadoOrdenCompra.PENDIENTE_PAGO
                && factura != null && factura.getFormaDePago() != null
                && factura.getFormaDePago().getTipoPago() == TipoPago.BILLETERA_VIRTUAL);
        return "cliente/compras/detalle";
    }

    @PostMapping("/{id}/anular")
    public String anular(@PathVariable String id, RedirectAttributes flash) {
        OrdenCompra compra = buscarCompraPropia(id);
        try {
            ventaService.anularVenta(id, false);
            flash.addFlashAttribute("exito", "Anulaste la compra " + compra.getIdentificadorCompra() + ".");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    // 404 si no existe o es un carrito abierto; 403 si es de otro cliente.
    private OrdenCompra buscarCompraPropia(String id) {
        OrdenCompra compra;
        try {
            compra = ordenCompraService.buscarPedido(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        Cliente cliente = clienteActual();
        if (cliente == null || !ordenCompraService.esCompraDelCliente(compra, cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La compra no es tuya.");
        }
        return compra;
    }

    // /cliente/** exige el rol CLIENTE, así que siempre hay un usuario logueado. El cliente puede no existir todavía.
    private Cliente clienteActual() {
        Usuario usuario = usuarioService.usuarioActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return clienteService.buscarClientePorUsuario(usuario.getId()).orElse(null);
    }
}
