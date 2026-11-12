package com.dulcemayor.service.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/** Boundary around Mercado Pago's Orders API, injectable as a mock in tests. */
public interface MercadoPagoOrdersClient {
    OrderResponse crearOrden(CreateOrderRequest request, String idempotencyKey);
    OrderResponse buscarOrden(String orderId);

    record CreateOrderRequest(
            String type,
            @JsonProperty("processing_mode") String processingMode,
            @JsonProperty("capture_mode") String captureMode,
            @JsonProperty("total_amount") String totalAmount,
            @JsonProperty("external_reference") String externalReference,
            String description,
            Payer payer,
            List<Item> items,
            Config config) {}

    record Payer(String email) {}
    record Item(String title, @JsonProperty("unit_price") String unitPrice, int quantity) {}
    record Config(Online online) {}
    record Online(@JsonProperty("success_url") String successUrl,
            @JsonProperty("failure_url") String failureUrl,
            @JsonProperty("pending_url") String pendingUrl,
            @JsonProperty("auto_return") String autoReturn) {}

    record OrderResponse(String id, String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("checkout_url") String checkoutUrl,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("total_amount") BigDecimal totalAmount,
            @JsonProperty("total_paid_amount") BigDecimal totalPaidAmount,
            String type, Transactions transactions) {}

    record Transactions(List<PaymentTransaction> payments) {}
    record PaymentTransaction(String id, String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("paid_amount") BigDecimal paidAmount) {}
}
