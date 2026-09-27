package com.zero.ecommerce.dto;

import java.util.List;

/**
 * Resultado del reporte de ventas (RF28, E5-01).
 * Incluye los totales del período, el detalle por línea de venta y subtotales por forma de pago.
 */
public record ReporteVentasDTO(
        int cantidadCompras,
        int unidadesVendidas,
        double montoTotal,
        List<DetalleVentaDTO> detalle,
        List<SubtotalFormaPagoDTO> subtotalesPorFormaPago
) {}
