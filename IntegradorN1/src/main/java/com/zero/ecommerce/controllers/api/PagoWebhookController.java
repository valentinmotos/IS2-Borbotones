package com.zero.ecommerce.controllers.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import com.zero.ecommerce.services.MercadoPagoService;

/**
 * Webhook público de Mercado Pago (E4-10), exento de CSRF y autenticación. Solo procesa notificaciones de pagos
 * (type=payment) con la firma válida; el estado real del pago lo consulta el service a la API.
 */
@RestController
@RequestMapping("/webhooks/mercadopago")
public class PagoWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagoWebhookController.class);

    private final MercadoPagoService mercadoPagoService;
    private final String webhookSecret;

    public PagoWebhookController(MercadoPagoService mercadoPagoService,
            @Value("${mercadopago.webhook-secret:}") String webhookSecret) {
        this.mercadoPagoService = mercadoPagoService;
        this.webhookSecret = webhookSecret;
    }

    /**
     * Responde 200 rápido en todos los casos esperables (incluidas las notificaciones que no son de pagos), 401 si la
     * firma no es válida y 500 solo si no se pudo consultar a Mercado Pago o guardar el pago, para que reintente.
     */
    @PostMapping
    public ResponseEntity<Void> recibirNotificacion(
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestHeader(name = "x-signature", required = false) String firma,
            @RequestHeader(name = "x-request-id", required = false) String requestId) {

        LOGGER.info("Notificación de Mercado Pago: type={}, topic={}, data.id={}, x-request-id={}",
                type, topic, dataId, requestId);

        if (!"payment".equals(type) || dataId == null || dataId.isBlank()) {
            // merchant_order, IPN (topic/id) u otros tópicos: no cambian la orden.
            return ResponseEntity.ok().build();
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            LOGGER.warn("Falta la clave secreta del webhook (MP_WEBHOOK_SECRET): no se valida la firma.");
        } else {
            try {
                WebhookSignatureValidator.validate(firma, requestId, dataId, webhookSecret);
            } catch (MPInvalidWebhookSignatureException e) {
                LOGGER.warn("Firma inválida en la notificación del pago {} ({}).", dataId, e.getReason());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            mercadoPagoService.procesarPago(dataId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOGGER.error("No se pudo procesar el pago {}; Mercado Pago va a reintentar: {}", dataId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
