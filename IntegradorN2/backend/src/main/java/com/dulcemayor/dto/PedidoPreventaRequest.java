package com.dulcemayor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PedidoPreventaRequest(
        UUID claveCarga,
        @NotBlank(message = "Seleccioná un punto de venta.") String puntoDeVentaId,
        @Size(max = 50, message = "El código admite hasta 50 caracteres.") String codigoPromocion,
        @NotEmpty(message = "Agregá al menos un producto.")
        @Size(max = 100, message = "El pedido admite hasta 100 productos.")
        List<@NotNull @Valid Linea> lineas) {

    public record Linea(
            @NotBlank(message = "Seleccioná un producto.") String productoId,
            @Positive(message = "Ingresá una cantidad mayor que cero.") int cantidad) {}
}
