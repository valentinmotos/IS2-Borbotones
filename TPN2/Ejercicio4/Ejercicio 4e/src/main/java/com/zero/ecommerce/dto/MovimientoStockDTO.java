package com.zero.ecommerce.dto;

import java.time.LocalDateTime;

/**
 * Una fila del historial de stock de un producto. cantidad es lo que movió (positiva si entró, negativa si salió) y
 * saldo es el stock que quedó. comprobante nombra la factura de origen ("Compra N.º 3"), o es null si el movimiento
 * no tiene factura (el stock inicial de demostración de E3-06); compraId solo viene si la factura es una compra a
 * proveedor, para enlazar su detalle.
 */
public record MovimientoStockDTO(LocalDateTime fecha, String observacion, String comprobante, String compraId,
        int cantidad, int saldo) {
}
