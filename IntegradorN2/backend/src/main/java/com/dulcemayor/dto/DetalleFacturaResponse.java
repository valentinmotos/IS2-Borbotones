package com.dulcemayor.dto;

/** E4-04: una línea de la factura (copia congelada de la línea del pedido al facturar). */
public record DetalleFacturaResponse(String productoId, String codigo, String producto, String presentacion,
        int cantidad, double precioUnitario, double subtotal) {
}
