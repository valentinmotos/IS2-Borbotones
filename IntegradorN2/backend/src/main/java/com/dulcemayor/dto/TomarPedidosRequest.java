package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** E5-02: pedidos que el distribuidor toma para su ruta de reparto del día. */
public record TomarPedidosRequest(
        @NotEmpty(message = "Elegí al menos un pedido para repartir.")
        @Size(max = 100, message = "Se pueden tomar hasta 100 pedidos por vez.")
        List<@NotBlank(message = "Elegí al menos un pedido para repartir.") String> pedidoIds) {
}
