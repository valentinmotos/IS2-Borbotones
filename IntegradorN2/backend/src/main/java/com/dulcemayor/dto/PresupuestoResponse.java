package com.dulcemayor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * E3-04: presupuesto armado a partir del mensaje del chat.
 *
 * @param items            productos con stock y precio, con su subtotal en pesos
 * @param excluidos        productos pedidos que no entran en el presupuesto, con el motivo
 * @param total            suma de los subtotales, en pesos
 * @param cotizacionDolar  dólar oficial vendedor usado para el total en dólares; null si no hay cotización
 * @param fechaCotizacion  día de esa cotización; null si no hay cotización
 * @param totalDolares     total / cotización, redondeado a 2 decimales; null si no hay cotización
 */
public record PresupuestoResponse(List<ItemPresupuestoDTO> items, List<ProductoExcluidoDTO> excluidos,
        BigDecimal total, Double cotizacionDolar, LocalDate fechaCotizacion, BigDecimal totalDolares) {
}
