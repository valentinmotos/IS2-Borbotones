package com.dulcemayor.dto;

import java.math.BigDecimal;

public record CheckoutPagoResponse(String facturaId, String ordenMercadoPagoId, String checkoutUrl,
        String estado, BigDecimal monto, String moneda) {
}
