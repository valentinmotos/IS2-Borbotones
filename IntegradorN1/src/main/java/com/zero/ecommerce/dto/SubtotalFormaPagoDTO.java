package com.zero.ecommerce.dto;

/**
 * Subtotal acumulado por forma de pago para el reporte de ventas (E5-01, RF28).
 */
public record SubtotalFormaPagoDTO(
        String formaPago,
        int cantidadCompras,
        int unidadesVendidas,
        double montoTotal
) {}
