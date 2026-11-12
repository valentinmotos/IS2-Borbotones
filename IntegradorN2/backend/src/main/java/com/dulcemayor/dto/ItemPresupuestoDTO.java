package com.dulcemayor.dto;

import java.math.BigDecimal;

/**
 * E3-04: renglón válido del presupuesto del chatbot. El precio unitario es el precio mayorista
 * vigente (E2-03) y lo calcula el backend, nunca la IA.
 */
public record ItemPresupuestoDTO(String idProducto, String codigo, String nombre, String presentacion,
        int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
}
