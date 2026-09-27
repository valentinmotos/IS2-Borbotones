package com.zero.ecommerce.controllers.cliente;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ClienteService;
import com.zero.ecommerce.services.MercadoPagoService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.UsuarioService;

/**
 * Controlador para la integración del cliente con Mercado Pago (E4-10).
 * Maneja el inicio del pago (creación de preferencia y redirección) y la pantalla de resultado.
 */
@Controller
@RequestMapping("/cliente/pago")
public class PagoClienteController {

    private final OrdenCompraService ordenCompraService;
    private final MercadoPagoService mercadoPagoService;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public PagoClienteController(
            OrdenCompraService ordenCompraService,
            MercadoPagoService mercadoPagoService,
            ClienteService clienteService,
            UsuarioService usuarioService) {
        this.ordenCompraService = ordenCompraService;
        this.mercadoPagoService = mercadoPagoService;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    /**
     * URL del contrato E4-10: verifica que la orden sea del cliente logueado y esté en PENDIENTE_PAGO,
     * crea una nueva preferencia en Mercado Pago y redirige al usuario a la pasarela.
     */
    @GetMapping("/{idOrden}")
    public String iniciarPago(@PathVariable String idOrden, RedirectAttributes flash) {
        OrdenCompra orden;
        try {
            orden = ordenCompraService.buscarPedido(idOrden);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        Cliente cliente = clienteActual();
        if (cliente == null || !ordenCompraService.esCompraDelCliente(orden, cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La compra no es tuya.");
        }

        if (orden.getEstadoOrdenCompra() != EstadoOrdenCompra.PENDIENTE_PAGO) {
            flash.addFlashAttribute("error", "La compra ya fue pagada o no se encuentra en estado pendiente de pago.");
            return "redirect:/cliente/compras/" + idOrden;
        }

        try {
            String urlPago = mercadoPagoService.crearPreferencia(orden);
            return "redirect:" + urlPago;
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/cliente/compras/" + idOrden;
        }
    }

    /**
     * Página de retorno de Mercado Pago tras completar o cancelar el flujo de pago.
     */
    @GetMapping("/resultado")
    public String resultadoPago(
            @RequestParam(required = false) String collection_status,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String external_reference,
            @RequestParam(required = false) String payment_id,
            @RequestParam(required = false) String collection_id,
            Model model) {

        String estadoResultado = status != null ? status : collection_status;
        if (estadoResultado == null) {
            estadoResultado = "desconocido";
        }

        String idPagoPrueba = payment_id != null && !payment_id.isBlank() ? payment_id : collection_id;
        if (idPagoPrueba != null && !idPagoPrueba.isBlank() && "approved".equalsIgnoreCase(estadoResultado)) {
            try {
                mercadoPagoService.procesarNotificacion(idPagoPrueba);
            } catch (Exception e) {
                // Si falla el procesamiento en el retorno, el webhook se encargará
            }
        }

        OrdenCompra compra = null;
        if (external_reference != null && !external_reference.isBlank()) {
            compra = ordenCompraService.buscarPorIdentificadorCompra(external_reference).orElse(null);
        }

        model.addAttribute("pageTitle", "Resultado del pago");
        model.addAttribute("resultado", estadoResultado.toLowerCase());
        model.addAttribute("externalReference", external_reference);
        model.addAttribute("paymentId", idPagoPrueba);
        model.addAttribute("compra", compra);

        return "cliente/pago-resultado";
    }

    private Cliente clienteActual() {
        Usuario usuario = usuarioService.usuarioActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return clienteService.buscarClientePorUsuario(usuario.getId()).orElse(null);
    }
}
