package com.zero.ecommerce.dto;

import java.time.LocalDate;

/**
 * Detalle de cada renglón de venta para el reporte de ventas (E5-01, RF28).
 */
public record DetalleVentaDTO(
        LocalDate fechaCompra,
        String producto,
        String categoria,
        int cantidad,
        double precioUnitario,
        double subtotal,
        String identificadorCompra,
        String formaPago,
        String idFactura,
        String idOrden
) {}
