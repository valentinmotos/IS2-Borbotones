package com.dulcemayor.service;

/**
 * E5-02: se publica al entregar un pedido, para avisarle al próximo comercio de la ruta que su pedido
 * está en camino. Lo atiende {@link PedidoEnCaminoCorreoService} después del commit.
 */
public record PedidoEnCaminoEvent(String pedidoId, String distribuidor) {
}
