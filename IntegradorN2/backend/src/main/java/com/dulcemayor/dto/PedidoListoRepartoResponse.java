package com.dulcemayor.dto;

/**
 * E5-02: pedido FACTURADO con la factura PAGADA que el distribuidor puede tomar para repartir.
 */
public record PedidoListoRepartoResponse(String id, long numeroPedido, long numeroFactura, String nombreComercio,
        String calle, String numeracion, String localidad, double total) {
}
