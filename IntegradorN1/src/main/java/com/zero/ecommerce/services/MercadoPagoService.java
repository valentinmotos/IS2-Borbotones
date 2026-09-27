package com.zero.ecommerce.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;

/**
 * Integración con Checkout Pro de Mercado Pago (E4-10): crea la preferencia de pago de una orden y procesa las
 * notificaciones de pago. Nunca confía en los datos que llegan en la notificación ni en la vuelta del cliente: siempre
 * consulta el pago a la API con el access token y recién ahí cambia el estado de la orden.
 */
@Service
public class MercadoPagoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoService.class);

    static final String MONEDA = "ARS";
    // Texto que ve el comprador en el resumen de la tarjeta (máximo 13 caracteres).
    private static final String DESCRIPTOR_RESUMEN = "ZERO";

    private final String accessToken;
    private final String appUrlBase;
    private final PreferenceClient preferenceClient;
    private final PaymentClient paymentClient;
    private final PaymentRefundClient paymentRefundClient;
    private final OrdenCompraService ordenCompraService;
    private final VentaService ventaService;

    public MercadoPagoService(
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${app.url-base:http://localhost:8080}") String appUrlBase,
            PreferenceClient preferenceClient,
            PaymentClient paymentClient,
            PaymentRefundClient paymentRefundClient,
            OrdenCompraService ordenCompraService,
            VentaService ventaService) {
        this.accessToken = accessToken;
        this.appUrlBase = normalizarUrlBase(appUrlBase);
        this.preferenceClient = preferenceClient;
        this.paymentClient = paymentClient;
        this.paymentRefundClient = paymentRefundClient;
        this.ordenCompraService = ordenCompraService;
        this.ventaService = ventaService;
    }

    /**
     * Crea una preferencia de pago para la orden, con un ítem por cada detalle de su factura, y devuelve la URL de
     * pago (init_point). Cada llamada crea una preferencia nueva.
     */
    public String crearPreferencia(OrdenCompra orden) throws ErrorServiceException {
        if (orden == null) {
            throw new ErrorServiceException("La orden de compra es nula.");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new ErrorServiceException("El pago con Mercado Pago no está configurado. Probá más tarde.");
        }

        PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(armarItems(orden))
                .payer(armarPagador(orden.getCliente()))
                .externalReference(orden.getIdentificadorCompra())
                .statementDescriptor(DESCRIPTOR_RESUMEN);

        // Mercado Pago descarta las back_urls y la notification_url que no son HTTPS públicas (por ejemplo,
        // localhost). En local hay que usar un túnel como ngrok y apuntar app.url-base a su URL.
        if (esUrlPublica()) {
            String urlResultado = appUrlBase + "/cliente/pago/resultado";
            builder.backUrls(PreferenceBackUrlsRequest.builder()
                            .success(urlResultado)
                            .pending(urlResultado)
                            .failure(urlResultado)
                            .build())
                    .autoReturn("approved")
                    // source_news=webhooks: solo notificaciones con formato Webhooks (las IPN no se pueden firmar).
                    .notificationUrl(appUrlBase + "/webhooks/mercadopago?source_news=webhooks");
        } else {
            LOGGER.warn("app.url-base ({}) no es una URL HTTPS pública: Mercado Pago no va a volver al sitio ni "
                    + "notificar el pago al webhook.", appUrlBase);
        }

        try {
            Preference preference = preferenceClient.create(builder.build());
            if (preference.getInitPoint() == null || preference.getInitPoint().isBlank()) {
                throw new ErrorServiceException("Mercado Pago no devolvió la URL de pago.");
            }
            return preference.getInitPoint();
        } catch (MPApiException e) {
            LOGGER.error("Mercado Pago rechazó la preferencia de la orden {} (status {}): {}",
                    orden.getIdentificadorCompra(), e.getStatusCode(),
                    e.getApiResponse() != null ? e.getApiResponse().getContent() : e.getMessage());
            throw new ErrorServiceException("No se pudo iniciar el pago en Mercado Pago. Probá de nuevo en unos minutos.");
        } catch (MPException e) {
            LOGGER.error("Error de conexión con Mercado Pago al crear la preferencia de la orden {}: {}",
                    orden.getIdentificadorCompra(), e.getMessage(), e);
            throw new ErrorServiceException("No se pudo iniciar el pago en Mercado Pago. Probá de nuevo en unos minutos.");
        }
    }

    /**
     * Procesa la notificación de un pago: lo consulta a la API y, según su estado, registra el pago de la orden
     * (approved) o la anula (refunded / charged_back). Es idempotente, porque Mercado Pago puede notificar el mismo pago
     * varias veces. Es synchronized para que dos notificaciones simultáneas del mismo pago (webhook y vuelta del
     * cliente) no descuenten el stock dos veces.
     *
     * @throws MPException    si no se pudo consultar a Mercado Pago; el webhook responde error para que reintente.
     */
    public synchronized void procesarPago(String idPago) throws MPException {
        Long id = parsearId(idPago);
        if (id == null) {
            LOGGER.warn("Notificación de Mercado Pago con un id de pago inválido: {}", idPago);
            return;
        }

        Payment pago;
        try {
            pago = paymentClient.get(id);
        } catch (MPApiException e) {
            if (e.getStatusCode() == 404) {
                LOGGER.warn("Mercado Pago no encontró el pago {}: se ignora la notificación.", id);
                return;
            }
            throw new MPException("Mercado Pago respondió " + e.getStatusCode() + " al consultar el pago " + id, e);
        }

        LOGGER.info("Pago {} consultado a Mercado Pago: estado {} ({}), external_reference {}, monto {} {}",
                id, pago.getStatus(), pago.getStatusDetail(), pago.getExternalReference(),
                pago.getTransactionAmount(), pago.getCurrencyId());

        OrdenCompra orden = ordenCompraService.buscarPorIdentificadorCompra(pago.getExternalReference()).orElse(null);
        if (orden == null) {
            LOGGER.warn("El pago {} no corresponde a ninguna orden (external_reference {}).", id,
                    pago.getExternalReference());
            return;
        }

        String estado = pago.getStatus() == null ? "" : pago.getStatus();
        switch (estado) {
            case "approved" -> registrarPagoAprobado(pago, orden);
            case "refunded", "charged_back" -> anularPorDevolucion(pago, orden);
            case "in_mediation" -> LOGGER.warn("El pago {} de la orden {} tiene un reclamo abierto (in_mediation). "
                    + "Revisarlo en el panel de Mercado Pago.", id, orden.getIdentificadorCompra());
            // pending, in_process, rejected, cancelled...: la orden sigue en PENDIENTE_PAGO, lista para reintentar.
            default -> LOGGER.info("El pago {} de la orden {} está en {}: la orden no cambia.", id,
                    orden.getIdentificadorCompra(), estado);
        }
    }

    private void registrarPagoAprobado(Payment pago, OrdenCompra orden) throws MPException {
        BigDecimal esperado;
        try {
            esperado = totalACobrar(orden);
        } catch (ErrorServiceException e) {
            LOGGER.error("El pago aprobado {} es de la orden {}, pero no se pudo calcular su total: {}", pago.getId(),
                    orden.getIdentificadorCompra(), e.getMessage());
            return;
        }
        if (!MONEDA.equals(pago.getCurrencyId()) || pago.getTransactionAmount() == null
                || pago.getTransactionAmount().compareTo(esperado) != 0) {
            LOGGER.error("El pago aprobado {} no coincide con la orden {}: se esperaban {} {} y llegaron {} {}. "
                    + "No se registra; revisarlo en el panel de Mercado Pago.", pago.getId(),
                    orden.getIdentificadorCompra(), esperado, MONEDA, pago.getTransactionAmount(),
                    pago.getCurrencyId());
            return;
        }

        try {
            ventaService.registrarPago(orden.getId());
            LOGGER.info("Pago {} registrado en la orden {}.", pago.getId(), orden.getIdentificadorCompra());
        } catch (ErrorServiceException e) {
            // La orden no admite el pago (por ejemplo, se quedó sin stock o el cliente la anuló mientras pagaba).
            // Mercado Pago ya cobró, así que se devuelve el dinero y se anula la orden.
            LOGGER.error("No se pudo registrar el pago {} en la orden {} ({}). Se devuelve el dinero.", pago.getId(),
                    orden.getIdentificadorCompra(), e.getMessage());
            devolverPago(pago, orden);
        }
    }

    private void devolverPago(Payment pago, OrdenCompra orden) throws MPException {
        try {
            paymentRefundClient.refund(pago.getId());
        } catch (MPApiException e) {
            throw new MPException("Mercado Pago respondió " + e.getStatusCode() + " al devolver el pago "
                    + pago.getId(), e);
        }
        LOGGER.warn("Pago {} devuelto al comprador de la orden {}.", pago.getId(), orden.getIdentificadorCompra());
        try {
            ventaService.anularVenta(orden.getId(), true);
        } catch (ErrorServiceException e) {
            LOGGER.info("La orden {} no se anula: {}", orden.getIdentificadorCompra(), e.getMessage());
        }
    }

    /** Si devolvieron el pago o hubo un contracargo, la orden pagada se anula y el stock vuelve (E4-03). */
    private void anularPorDevolucion(Payment pago, OrdenCompra orden) {
        if (orden.getEstadoOrdenCompra() != EstadoOrdenCompra.PENDIENTE_ENVIO) {
            if (orden.getEstadoOrdenCompra() != EstadoOrdenCompra.ANULADA) {
                LOGGER.warn("El pago {} de la orden {} quedó en {}, pero la orden está en {} y no se puede anular "
                        + "automáticamente. Revisarla a mano.", pago.getId(), orden.getIdentificadorCompra(),
                        pago.getStatus(), orden.getEstadoOrdenCompra());
            }
            return;
        }
        try {
            ventaService.anularVenta(orden.getId(), true);
            LOGGER.warn("Orden {} anulada porque el pago {} quedó en {}.", orden.getIdentificadorCompra(),
                    pago.getId(), pago.getStatus());
        } catch (ErrorServiceException e) {
            LOGGER.error("No se pudo anular la orden {} tras el pago {} en {}: {}", orden.getIdentificadorCompra(),
                    pago.getId(), pago.getStatus(), e.getMessage());
        }
    }

    /** Un ítem por cada detalle activo de la factura, con el precio congelado al confirmar la compra. */
    private List<PreferenceItemRequest> armarItems(OrdenCompra orden) throws ErrorServiceException {
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(orden.getId())
                .orElseThrow(() -> new ErrorServiceException("El pedido no tiene una factura asociada."));
        List<PreferenceItemRequest> items = new ArrayList<>();
        for (DetalleFactura detalle : factura.getDetalles()) {
            if (detalle.isEliminado() || detalle.getProducto() == null) {
                continue;
            }
            items.add(PreferenceItemRequest.builder()
                    .id(detalle.getProducto().getId())
                    .title(detalle.getProducto().getNombre())
                    .description(detalle.getProducto().getDescripcion())
                    .quantity(detalle.getCantidad())
                    .unitPrice(precio(detalle.getPrecioUnitario()))
                    .currencyId(MONEDA)
                    .build());
        }
        if (items.isEmpty()) {
            throw new ErrorServiceException("No se puede generar un pago para una orden sin ítems.");
        }
        return items;
    }

    /** Lo que tiene que cobrar Mercado Pago: la suma de los ítems tal como se mandan en la preferencia. */
    BigDecimal totalACobrar(OrdenCompra orden) throws ErrorServiceException {
        return armarItems(orden).stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PreferencePayerRequest armarPagador(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        return PreferencePayerRequest.builder()
                .name(cliente.getNombre())
                .surname(cliente.getApellido())
                .email(cliente.getUsuario() != null ? cliente.getUsuario().getNombreUsuario() : null)
                .build();
    }

    private boolean esUrlPublica() {
        return appUrlBase.startsWith("https://") && !appUrlBase.contains("localhost")
                && !appUrlBase.contains("127.0.0.1");
    }

    private static BigDecimal precio(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static Long parsearId(String idPago) {
        if (idPago == null || idPago.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(idPago.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizarUrlBase(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
