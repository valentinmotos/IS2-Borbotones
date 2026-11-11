package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MovimientoStockRequest(
        @NotBlank String productoId,
        @NotNull @Positive Integer cantidad,
        @Size(max = 500) String observacion) {
}
