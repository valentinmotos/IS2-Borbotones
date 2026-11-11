package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** E2-02: ajuste manual del saldo. La diferencia es positiva para sumar y negativa para descontar. */
public record AjusteStockRequest(
        @NotBlank(message = "Seleccioná un producto.") String productoId,
        @NotNull(message = "Ingresá la diferencia de unidades.") Integer diferencia,
        @Size(max = 500, message = "La observación admite hasta 500 caracteres.") String observacion) {
}
