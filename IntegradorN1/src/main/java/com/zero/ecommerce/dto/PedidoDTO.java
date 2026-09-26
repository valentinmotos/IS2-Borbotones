package com.zero.ecommerce.dto;

import java.time.LocalDate;

/**
 * Fila del listado de pedidos del panel (E4-06). Junta datos de la OrdenCompra y de su FacturaCliente, porque la forma
 * de pago está en la factura. formaDePago es null si el pedido todavía no tiene factura. estado es el nombre del
 * EstadoOrdenCompra, para el badge-estado del kit.
 */
public record PedidoDTO(String id, String numero, LocalDate fecha, String cliente, double total, String formaDePago,
        String estado) {
}
