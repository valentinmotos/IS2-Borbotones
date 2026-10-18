package com.zero.ecommerce.services;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.ItemCorreoCompraDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.DetalleCompra;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;

/**
 * Correos de la compra al cliente (E4-05): la confirmación (RF20) y el aviso de cada cambio de estado. Los datos se
 * arman acá, en el hilo que llama (con la sesión de Hibernate abierta), y el envío va por {@link EmailService#enviar},
 * que es asincrónico: si el correo falla queda en el log y la compra o el pago siguen su curso.
 */
@Service
@Transactional(readOnly = true)
public class NotificacionCompraService {

    public static final String ASUNTO_CONFIRMACION = "Recibimos tu compra";
    public static final String ASUNTO_CAMBIO_ESTADO = "Novedades de tu compra";
    private static final Logger log = LoggerFactory.getLogger(NotificacionCompraService.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;
    private final OrdenCompraService ordenCompraService;
    private final String urlBase;

    public NotificacionCompraService(EmailService emailService, OrdenCompraService ordenCompraService,
            @Value("${app.url-base:http://localhost:8080}") String urlBase) {
        this.emailService = emailService;
        this.ordenCompraService = ordenCompraService;
        this.urlBase = normalizarUrlBase(urlBase);
    }

    /**
     * Confirmación de la compra (RF20): número, fecha, ítems con cantidad y precio, total, forma de pago, instrucciones
     * de pago si corresponde y link al seguimiento. Se llama al confirmar el checkout (E4-02).
     */
    public void enviarConfirmacion(OrdenCompra orden) {
        String destinatario = buscarDestinatario(orden);
        if (destinatario == null) {
            return;
        }
        Optional<FacturaCliente> factura = ordenCompraService.buscarFacturaDePedido(orden.getId());
        FormaDePago formaDePago = factura.map(FacturaCliente::getFormaDePago).orElse(null);

        Map<String, Object> variables = variablesComunes(orden);
        variables.put("fecha", orden.getFecha() == null ? "-" : orden.getFecha().format(FORMATO_FECHA));
        variables.put("items", listarItems(orden));
        variables.put("total", formatearImporte(orden.getTotal()));
        variables.put("numeroFactura", factura.map(f -> String.valueOf(f.getNumeroFactura())).orElse(null));
        variables.put("formaDePago", formaDePago == null ? "-" : formaDePago.getObservacion());
        variables.put("instruccionesPago", instruccionesPago(orden, formaDePago));
        emailService.enviar(destinatario, ASUNTO_CONFIRMACION, "confirmacion-compra", variables);
    }

    /**
     * Aviso del nuevo estado de la orden con link al seguimiento. Se llama después de cada transición: pago registrado
     * (E4-03), envío, entrega y anulación desde el panel (E4-07).
     */
    public void notificarCambioEstado(OrdenCompra orden) {
        String destinatario = buscarDestinatario(orden);
        if (destinatario == null) {
            return;
        }
        Map<String, Object> variables = variablesComunes(orden);
        variables.put("estado", describirEstado(orden.getEstadoOrdenCompra()));
        variables.put("mensajeEstado", mensajeEstado(orden.getEstadoOrdenCompra()));
        emailService.enviar(destinatario, ASUNTO_CAMBIO_ESTADO, "cambio-estado", variables);
    }

    private Map<String, Object> variablesComunes(OrdenCompra orden) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCliente", orden.getCliente().getNombre());
        variables.put("identificador", orden.getIdentificadorCompra());
        variables.put("urlSeguimiento", urlBase + "/cliente/compras/" + orden.getId());
        return variables;
    }

    // El correo del cliente es el nombre de usuario. Sin cliente o sin usuario no hay a quién avisarle.
    private String buscarDestinatario(OrdenCompra orden) {
        Cliente cliente = orden == null ? null : orden.getCliente();
        if (cliente == null || cliente.getUsuario() == null) {
            log.warn("No se envía el correo de la compra {}: la orden no tiene un cliente con usuario.",
                    orden == null ? null : orden.getIdentificadorCompra());
            return null;
        }
        return cliente.getUsuario().getNombreUsuario();
    }

    private List<ItemCorreoCompraDTO> listarItems(OrdenCompra orden) {
        return orden.getDetalles().stream()
                .filter(d -> !d.isEliminado())
                .map(this::armarItem)
                .toList();
    }

    private ItemCorreoCompraDTO armarItem(DetalleCompra detalle) {
        String producto = detalle.getProducto().getNombre() + " (talle " + detalle.getProducto().getTalle() + ")";
        return new ItemCorreoCompraDTO(producto, detalle.getCantidad(), formatearImporte(detalle.getPrecioUnitario()),
                formatearImporte(detalle.getSubtotal()));
    }

    /**
     * Instrucciones de pago de una orden pendiente de pago, o null si no corresponde. El diagrama no guarda datos de
     * pago de la tienda: son un texto fijo por tipo de pago. También las muestra la página "Compra registrada" (E4-02).
     */
    public String instruccionesPago(OrdenCompra orden, FormaDePago formaDePago) {
        if (orden.getEstadoOrdenCompra() != EstadoOrdenCompra.PENDIENTE_PAGO || formaDePago == null
                || formaDePago.getTipoPago() == null) {
            return null;
        }
        return switch (formaDePago.getTipoPago()) {
            case EFECTIVO -> "Aboná el total en efectivo en nuestro local indicando el número de compra. "
                    + "Cuando registremos el pago te avisamos por correo y preparamos tu pedido.";
            case TRANSFERENCIA -> "Hacé una transferencia por el total y enviá el comprobante respondiendo este correo, "
                    + "con el número de compra. Cuando se acredite te avisamos y preparamos tu pedido.";
            case BILLETERA_VIRTUAL -> "Mercado Pago simulado: el pago está pendiente de confirmación administrativa. "
                    + "No se realiza ningún cobro real.";
        };
    }

    private String describirEstado(EstadoOrdenCompra estado) {
        if (estado == null) {
            return "-";
        }
        return ordenCompraService.listarEstadoPedido().getOrDefault(estado.name(), estado.name());
    }

    private String mensajeEstado(EstadoOrdenCompra estado) {
        if (estado == null) {
            return "El estado de tu compra cambió.";
        }
        return switch (estado) {
            case PENDIENTE_PAGO -> "Tu compra está registrada y espera el pago.";
            case PENDIENTE_ENVIO -> "¡Recibimos tu pago! Ya estamos preparando tu pedido.";
            case PENDIENTE_ENTREGA -> "Tu pedido ya está en camino.";
            case ENTREGADO -> "Tu pedido fue entregado. ¡Gracias por comprar en Zero!";
            case ANULADA -> "Tu compra fue anulada. Si tenés dudas, respondé este correo.";
            default -> "El estado de tu compra cambió.";
        };
    }

    private String formatearImporte(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.of("es", "AR"));
        formato.setMaximumFractionDigits(2);
        return "$" + formato.format(valor);
    }

    private String normalizarUrlBase(String valor) {
        String base = valor == null || valor.isBlank() ? "http://localhost:8080" : valor.strip();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
