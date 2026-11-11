package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AjusteStockRequest(
        @NotBlank String productoId,
        @NotNull Integer diferencia,
        @Size(max = 500) String observacion) {
}
