package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** E2-02: ingreso de mercadería (reposición) de un producto. */
public record MovimientoStockRequest(
        @NotBlank(message = "Seleccioná un producto.") String productoId,
        @NotNull(message = "Ingresá la cantidad.") @Positive(message = "La cantidad del ingreso debe ser mayor que cero.")
        Integer cantidad,
        @Size(max = 500, message = "La observación admite hasta 500 caracteres.") String observacion) {
}
