package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.MonedaPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PagoRequest(
        @NotBlank(message = "Indicá la factura.") String facturaId,
        @NotBlank(message = "Indicá la forma de pago.") String formaDePagoId,
        @NotNull(message = "Indicá el monto.") @Positive(message = "El monto debe ser mayor que cero.") Double monto,
        @NotNull(message = "Indicá la moneda.") MonedaPago moneda,
        String observacion) {
}
