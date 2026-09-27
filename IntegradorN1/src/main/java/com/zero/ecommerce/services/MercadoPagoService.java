package com.zero.ecommerce.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.zero.ecommerce.entities.DetalleCompra;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;

/**
 * Servicio de integración con el SDK de Mercado Pago (E4-10).
 * Maneja la creación de preferencias de pago y el procesamiento de notificaciones (webhooks).
 */
@Service
public class MercadoPagoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoService.class);

    private final String accessToken;
    private final String appUrlBase;
    private final OrdenCompraService ordenCompraService;
    private final VentaService ventaService;

    public MercadoPagoService(
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${app.url-base:http://localhost:8080}") String appUrlBase,
            OrdenCompraService ordenCompraService,
            VentaService ventaService) {
        this.accessToken = accessToken;
        this.appUrlBase = normalizarUrlBase(appUrlBase);
        this.ordenCompraService = ordenCompraService;
        this.ventaService = ventaService;
    }

    /**
     * Crea una preferencia de pago en Mercado Pago para la orden indicada (E4-10).
     * Devuelve la URL de pago de la preferencia (init_point o sandbox_init_point).
     */
    public String crearPreferencia(OrdenCompra orden) throws ErrorServiceException {
        if (orden == null) {
            throw new ErrorServiceException("La orden de compra es nula.");
        }

        inicializarAccessToken();

        List<PreferenceItemRequest> items = new ArrayList<>();
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(orden.getId()).orElse(null);

        if (factura != null && !factura.getDetalles().isEmpty()) {
            for (DetalleFactura detalle : factura.getDetalles()) {
                if (!detalle.isEliminado() && detalle.getProducto() != null) {
                    items.add(PreferenceItemRequest.builder()
                            .title(detalle.getProducto().getNombre())
                            .quantity(detalle.getCantidad())
                            .unitPrice(BigDecimal.valueOf(detalle.getPrecioUnitario()).setScale(2, java.math.RoundingMode.HALF_UP))
                            .currencyId("ARS")
                            .build());
                }
            }
        } else if (orden.getDetalles() != null) {
            for (DetalleCompra detalle : orden.getDetalles()) {
                if (!detalle.isEliminado() && detalle.getProducto() != null) {
                    items.add(PreferenceItemRequest.builder()
                            .title(detalle.getProducto().getNombre())
                            .quantity(detalle.getCantidad())
                            .unitPrice(BigDecimal.valueOf(detalle.getPrecioUnitario()).setScale(2, java.math.RoundingMode.HALF_UP))
                            .currencyId("ARS")
                            .build());
                }
            }
        }

        if (items.isEmpty()) {
            throw new ErrorServiceException("No se puede generar un pago para una orden sin ítems.");
        }

        boolean esUrlPublica = !appUrlBase.contains("localhost") && !appUrlBase.contains("127.0.0.1");

        String urlResultado = appUrlBase + "/cliente/pago/resultado";
        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(urlResultado)
                .pending(urlResultado)
                .failure(urlResultado)
                .build();

        PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(items)
                .externalReference(orden.getIdentificadorCompra())
                .backUrls(backUrls);

        if (esUrlPublica) {
            builder.autoReturn("approved");
            builder.notificationUrl(appUrlBase + "/webhooks/mercadopago");
        }

        PreferenceRequest preferenceRequest = builder.build();

        try {
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);
            String urlPago = preference.getInitPoint();
            if (urlPago == null || urlPago.isBlank()) {
                urlPago = preference.getSandboxInitPoint();
            }
            return urlPago;
        } catch (com.mercadopago.exceptions.MPApiException apiException) {
            String detalle = apiException.getApiResponse() != null ? apiException.getApiResponse().getContent() : apiException.getMessage();
            LOGGER.error("MPApiException al crear preferencia en Mercado Pago (Status {}): {}",
                    apiException.getStatusCode(), detalle, apiException);
            throw new ErrorServiceException("Error en Mercado Pago API (" + apiException.getStatusCode() + "): " + detalle);
        } catch (Exception e) {
            LOGGER.error("Error al crear preferencia en Mercado Pago para la orden {}: {}",
                    orden.getIdentificadorCompra(), e.getMessage(), e);
            throw new ErrorServiceException("No se pudo iniciar el pago en Mercado Pago: " + e.getMessage());
        }
    }

    /**
     * Procesa la notificación enviada por el Webhook de Mercado Pago.
     * Consulta el pago a la API oficial por su ID y si está aprobado, registra el pago en la orden.
     */
    public void procesarNotificacion(String paymentId) {
        procesarNotificacion(paymentId, null);
    }

    public void procesarNotificacion(String paymentId, String fallbackExternalRef) {
        if ((paymentId == null || paymentId.isBlank()) && (fallbackExternalRef == null || fallbackExternalRef.isBlank())) {
            LOGGER.warn("Webhook Mercado Pago recibido sin ID de pago ni identificador de orden válido.");
            return;
        }

        LOGGER.info("Mercado Pago Webhook recibido - PaymentID: {}, ExternalRef: {}", paymentId, fallbackExternalRef);

        // 1. Simulación o actualización directa si paymentId o fallbackExternalRef coincide con una orden (por UUID o por ORD-...)
        String refDirecta = paymentId != null && !paymentId.isBlank() ? paymentId : fallbackExternalRef;
        if (refDirecta != null) {
            OrdenCompra ordenDirecta = ordenCompraService.buscarOrdenPorIdOIdentificador(refDirecta).orElse(null);
            if (ordenDirecta != null) {
                LOGGER.info("Procesando pago/actualización directa para la orden {} (ID: {})", refDirecta, ordenDirecta.getId());
                try {
                    ventaService.registrarPago(ordenDirecta.getId());
                    return;
                } catch (Exception e) {
                    LOGGER.error("Error al registrar pago para la orden {}: {}", refDirecta, e.getMessage(), e);
                }
            }
        }

        if (fallbackExternalRef != null && !fallbackExternalRef.isBlank()) {
            OrdenCompra ordenFallback = ordenCompraService.buscarOrdenPorIdOIdentificador(fallbackExternalRef).orElse(null);
            if (ordenFallback != null) {
                LOGGER.info("Procesando pago por external_reference para la orden {} (ID: {})", fallbackExternalRef, ordenFallback.getId());
                try {
                    ventaService.registrarPago(ordenFallback.getId());
                    return;
                } catch (Exception e) {
                    LOGGER.error("Error al registrar pago por external_reference {}: {}", fallbackExternalRef, e.getMessage(), e);
                }
            }
        }

        // 2. Consulta a la API oficial de Mercado Pago (Payment y MerchantOrder)
        try {
            inicializarAccessToken();

            if (paymentId != null && !paymentId.isBlank()) {
                try {
                    long idNumerico = Long.parseLong(paymentId);

                    // Intentar obtener Pago
                    try {
                        PaymentClient paymentClient = new PaymentClient();
                        Payment payment = paymentClient.get(idNumerico);
                        if (payment != null) {
                            String estado = payment.getStatus();
                            String externalReference = payment.getExternalReference();
                            System.out.println("=== WEBHOOK MP PAGO === ID: " + paymentId + " | Estado: " + estado + " | ExternalRef: " + externalReference);
                            LOGGER.info("Consulta API MP - Pago ID: {}, Estado: {}, External Reference: {}", paymentId, estado, externalReference);

                            if ("approved".equalsIgnoreCase(estado)) {
                                procesarOrdenPorRef(externalReference);
                                return;
                            }
                        }
                    } catch (Exception exPayment) {
                        LOGGER.debug("No se encontró como Payment ID {}", paymentId);
                    }

                    // Intentar obtener MerchantOrder
                    try {
                        com.mercadopago.client.merchantorder.MerchantOrderClient merchantOrderClient = new com.mercadopago.client.merchantorder.MerchantOrderClient();
                        com.mercadopago.resources.merchantorder.MerchantOrder merchantOrder = merchantOrderClient.get(idNumerico);
                        if (merchantOrder != null) {
                            String externalReference = merchantOrder.getExternalReference();
                            String orderStatus = merchantOrder.getOrderStatus();
                            System.out.println("=== WEBHOOK MP MERCHANT ORDER === ID: " + paymentId + " | Status: " + orderStatus + " | ExternalRef: " + externalReference);
                            LOGGER.info("Consulta API MP - MerchantOrder ID: {}, Status: {}, External Reference: {}", paymentId, orderStatus, externalReference);

                            boolean tienePagoAprobado = merchantOrder.getPayments() != null && merchantOrder.getPayments().stream()
                                    .anyMatch(p -> "approved".equalsIgnoreCase(p.getStatus()));

                            if ("paid".equalsIgnoreCase(orderStatus) || tienePagoAprobado) {
                                procesarOrdenPorRef(externalReference);
                                return;
                            }
                        }
                    } catch (Exception exMerchant) {
                        LOGGER.debug("No se encontró como MerchantOrder ID {}", paymentId);
                    }

                } catch (NumberFormatException nfe) {
                    LOGGER.warn("El ID de pago no es numérico largo: {}", paymentId);
                }
            }

            if (fallbackExternalRef != null && !fallbackExternalRef.isBlank()) {
                procesarOrdenPorRef(fallbackExternalRef);
            }
        } catch (Exception e) {
            LOGGER.error("Error procesando webhook de Mercado Pago para pago ID {}: {}", paymentId, e.getMessage(), e);
        }
    }

    private void procesarOrdenPorRef(String reference) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        OrdenCompra orden = ordenCompraService.buscarOrdenPorIdOIdentificador(reference).orElse(null);
        if (orden != null) {
            System.out.println(">>> REGISTRANDO PAGO EXITOSO PARA ORDEN: " + orden.getIdentificadorCompra() + " (ID: " + orden.getId() + ") <<<");
            LOGGER.info("Registrando pago para la orden {} (ID: {})", reference, orden.getId());
            try {
                ventaService.registrarPago(orden.getId());
            } catch (Exception e) {
                LOGGER.error("Error al registrar pago para la orden {}: {}", reference, e.getMessage(), e);
            }
        } else {
            LOGGER.warn("No se encontró la orden en BD para la referencia: {}", reference);
        }
    }

    private void inicializarAccessToken() throws ErrorServiceException {
        if (accessToken == null || accessToken.isBlank()) {
            LOGGER.warn("El access token de Mercado Pago no está configurado (mercadopago.access-token).");
        }
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    private String normalizarUrlBase(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
