package com.zero.ecommerce.dto;

import java.time.LocalDate;

/**
 * Último precio de costo que le cobró un proveedor a Zero por un producto (reporte de proveedores, E5-05). Sale de la
 * compra recibida más reciente de ese proveedor con ese producto. diferenciaPorcentual es cuánto más caro es que el
 * proveedor recomendado (0 para el recomendado).
 */
public record PrecioProveedorDTO(String idProveedor, String razonSocial, double precioCosto, LocalDate fechaCompra,
        String idCompra, long numeroCompra, double diferenciaPorcentual, boolean recomendado) {
}
