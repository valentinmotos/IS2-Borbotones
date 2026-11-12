package com.dulcemayor.controller;

import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.service.CheckoutMercadoPagoService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Receptor público de notificaciones Order; la autenticidad se verifica antes de consultar MP. */
@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class MercadoPagoWebhookController {
    private final CheckoutMercadoPagoService checkout;

    public MercadoPagoWebhookController(CheckoutMercadoPagoService checkout) { this.checkout = checkout; }

    @PostMapping("/orders")
    public ResponseEntity<Void> orden(@RequestParam(name = "data.id", required = false) String queryDataId,
            @RequestHeader(name = "x-signature", required = false) String firma,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) JsonNode body) {
        if (body == null || !"order".equals(body.path("type").asText())) return ResponseEntity.ok().build();
        String bodyDataId = body.path("data").path("id").asText(null);
        if (queryDataId == null || queryDataId.isBlank()) queryDataId = bodyDataId;
        if (queryDataId == null || queryDataId.isBlank()) throw new ErrorServiceException("Falta el identificador de la orden.");
        if (bodyDataId != null && !bodyDataId.equals(queryDataId)) {
            throw new ErrorServiceException("El identificador de la orden del webhook no coincide.");
        }
        checkout.procesarWebhook(queryDataId, firma, requestId, queryDataId);
        return ResponseEntity.ok().build();
    }
}
