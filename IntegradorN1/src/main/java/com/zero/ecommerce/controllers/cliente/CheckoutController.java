package com.zero.ecommerce.controllers.cliente;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CarritoService;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.NotificacionCompraService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.UsuarioService;
import com.zero.ecommerce.services.VentaService;

/**
 * Checkout del cliente (E4-02 / RF18): resumen del carrito, dirección de entrega, forma de pago y confirmación. Con
 * Mercado Pago redirige a la URL de pago del contrato con E4-10; con efectivo o transferencia muestra "Compra
 * registrada" con las instrucciones de pago.
 */
@Controller
@RequestMapping("/cliente/checkout")
public class CheckoutController {

    private static final String BASE = "/cliente/checkout";
    private static final String RUTA_CARRITO = "/cliente/carrito";
    private static final String RUTA_PERFIL = "/cliente/perfil";

    private final VentaService ventaService;
    private final CarritoService carritoService;
    private final ClienteService clienteService;
    private final FormaDePagoService formaDePagoService;
    private final OrdenCompraService ordenCompraService;
    private final NotificacionCompraService notificacionCompraService;
    private final UsuarioService usuarioService;

    public CheckoutController(VentaService ventaService, CarritoService carritoService, ClienteService clienteService,
            FormaDePagoService formaDePagoService, OrdenCompraService ordenCompraService,
            NotificacionCompraService notificacionCompraService, UsuarioService usuarioService) {
        this.ventaService = ventaService;
        this.carritoService = carritoService;
        this.clienteService = clienteService;
        this.formaDePagoService = formaDePagoService;
        this.ordenCompraService = ordenCompraService;
        this.notificacionCompraService = notificacionCompraService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String checkout(Model model, RedirectAttributes flash) {
        Usuario usuario = usuarioLogueado();
        if (!clienteService.perfilCompleto(usuario.getId())) {
            flash.addFlashAttribute("error", VentaService.MENSAJE_PERFIL_INCOMPLETO);
            return "redirect:" + RUTA_PERFIL;
        }
        try {
            Cliente cliente = clienteService.buscarClientePorUsuario(usuario.getId()).orElseThrow();
            // Igual que al abrir el carrito: se ajustan stock y precios antes de mostrar el resumen.
            List<String> avisos = carritoService.sincronizarAjustes(cliente.getId());
            OrdenCompra carrito = carritoService.obtenerCarrito(cliente.getId());
            if (carrito.getCantidadTotalItems() == 0) {
                flash.addFlashAttribute("error", "Tu carrito está vacío.");
                return "redirect:" + RUTA_CARRITO;
            }
            if (!avisos.isEmpty()) {
                // Hubo cambios: el cliente los revisa en el carrito antes de pagar.
                flash.addFlashAttribute("error", String.join(" ", avisos));
                return "redirect:" + RUTA_CARRITO;
            }
            model.addAttribute("pageTitle", "Confirmar compra");
            model.addAttribute("carrito", carrito);
            model.addAttribute("detalles", carrito.getDetalles().stream().filter(d -> !d.isEliminado()).toList());
            model.addAttribute("cliente", cliente);
            model.addAttribute("formasDePago", opcionesFormaDePago());
            return "cliente/checkout";
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:" + RUTA_CARRITO;
        }
    }

    @PostMapping
    public String confirmar(@RequestParam(required = false) String idFormaPago, RedirectAttributes flash) {
        Usuario usuario = usuarioLogueado();
        if (!clienteService.perfilCompleto(usuario.getId())) {
            flash.addFlashAttribute("error", VentaService.MENSAJE_PERFIL_INCOMPLETO);
            return "redirect:" + RUTA_PERFIL;
        }
        FacturaCliente factura;
        try {
            Cliente cliente = clienteService.buscarClientePorUsuario(usuario.getId()).orElseThrow();
            factura = ventaService.confirmarCompra(cliente.getId(), idFormaPago);
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        String idOrden = factura.getOrdenCompra().getId();
        if (factura.getFormaDePago().getTipoPago() == TipoPago.BILLETERA_VIRTUAL) {
            // URL del contrato con E4-10: crea la preferencia de pago y redirige a Mercado Pago.
            return "redirect:/cliente/pago/" + idOrden;
        }
        return "redirect:" + BASE + "/registrada/" + idOrden;
    }

    /** "Compra registrada" para efectivo y transferencia: número de compra e instrucciones de pago. */
    @GetMapping("/registrada/{idOrden}")
    public String registrada(@PathVariable String idOrden, Model model) {
        OrdenCompra compra;
        try {
            compra = ordenCompraService.buscarPedido(idOrden);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        Cliente cliente = clienteService.buscarClientePorUsuario(usuarioLogueado().getId()).orElse(null);
        if (cliente == null || !ordenCompraService.esCompraDelCliente(compra, cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La compra no es tuya.");
        }
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(idOrden).orElse(null);
        FormaDePago formaDePago = factura == null ? null : factura.getFormaDePago();
        model.addAttribute("pageTitle", "Compra registrada");
        model.addAttribute("compra", compra);
        model.addAttribute("factura", factura);
        model.addAttribute("instruccionesPago", notificacionCompraService.instruccionesPago(compra, formaDePago));
        return "cliente/compra-registrada";
    }

    // Formas de pago activas para el select del kit: id → "Observación (tipo de pago)".
    private Map<String, String> opcionesFormaDePago() {
        Map<String, String> opciones = new LinkedHashMap<>();
        for (FormaDePago forma : formaDePagoService.listarFormaDePagoActivo()) {
            opciones.put(forma.getId(), forma.getObservacion() + " (" + forma.getTipoPago().getDescripcion() + ")");
        }
        return opciones;
    }

    // /cliente/** exige el rol CLIENTE, así que siempre hay un usuario logueado.
    private Usuario usuarioLogueado() {
        return usuarioService.usuarioActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
