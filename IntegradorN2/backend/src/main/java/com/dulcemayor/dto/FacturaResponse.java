package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.EstadoFactura;
import java.time.LocalDate;
import java.util.List;

/**
 * E4-04: factura emitida. {@code numeroVisible} es el número con ceros a la izquierda que se imprime
 * en el PDF (p. ej. "00000012"); {@code subtotal} y {@code descuento} salen del pedido facturado.
 */
public record FacturaResponse(String id, long numeroFactura, String numeroVisible, LocalDate fechaFactura,
        double subtotal, double descuento, double total, EstadoFactura estado,
        String pedidoId, long numeroPedido, String comercio, List<DetalleFacturaResponse> detalles) {
}
