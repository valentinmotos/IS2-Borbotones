package com.zero.ecommerce.controllers.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zero.ecommerce.services.MercadoPagoService;

/**
 * Webhook público para recibir notificaciones de pago de Mercado Pago (E4-10).
 * Exento de CSRF y autenticación.
 */
@RestController
@RequestMapping("/webhooks/mercadopago")
public class PagoWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagoWebhookController.class);

    private final MercadoPagoService mercadoPagoService;

    public PagoWebhookController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }

    @PostMapping
    public ResponseEntity<Void> recibirNotificacionPost(
            @RequestParam(name = "id", required = false) String queryId,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "external_reference", required = false) String queryExternalRef,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestBody(required = false) Map<String, Object> body) {

        String paymentId = extraerPaymentId(queryId, dataId, body);
        String externalRef = extraerExternalRef(queryExternalRef, body);
        LOGGER.info("Notificación Webhook POST recibida en /webhooks/mercadopago. PaymentID: {}, ExternalRef: {}, Type: {}, Topic: {}, Body: {}",
                paymentId, externalRef, type, topic, body);

        if ((paymentId != null && !paymentId.isBlank()) || (externalRef != null && !externalRef.isBlank())) {
            try {
                mercadoPagoService.procesarNotificacion(paymentId, externalRef);
            } catch (Exception e) {
                LOGGER.error("Error al procesar notificación de Mercado Pago para pago ID {}: {}", paymentId, e.getMessage(), e);
            }
        }

        // Responde siempre 200 OK de inmediato para evitar reintentos masivos de Mercado Pago
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Void> recibirNotificacionGet(
            @RequestParam(name = "id", required = false) String queryId,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "topic", required = false) String topic) {

        String paymentId = queryId != null ? queryId : dataId;
        LOGGER.info("Notificación Webhook GET recibida en /webhooks/mercadopago. PaymentID: {}, Type: {}, Topic: {}",
                paymentId, type, topic);

        if (paymentId != null && !paymentId.isBlank()) {
            try {
                mercadoPagoService.procesarNotificacion(paymentId);
            } catch (Exception e) {
                LOGGER.error("Error al procesar notificación GET de Mercado Pago para pago ID {}: {}", paymentId, e.getMessage(), e);
            }
        }

        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private String extraerPaymentId(String queryId, String dataId, Map<String, Object> body) {
        if (queryId != null && !queryId.isBlank()) {
            return queryId;
        }
        if (dataId != null && !dataId.isBlank()) {
            return dataId;
        }
        if (body != null) {
            if (body.get("data") instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) body.get("data");
                if (dataMap.get("id") != null) {
                    return String.valueOf(dataMap.get("id"));
                }
            }
            if (body.get("id") != null) {
                return String.valueOf(body.get("id"));
            }
        }
        return null;
    }

    private String extraerExternalRef(String queryRef, Map<String, Object> body) {
        if (queryRef != null && !queryRef.isBlank()) {
            return queryRef;
        }
        if (body != null && body.get("external_reference") != null) {
            return String.valueOf(body.get("external_reference"));
        }
        return null;
    }
}
