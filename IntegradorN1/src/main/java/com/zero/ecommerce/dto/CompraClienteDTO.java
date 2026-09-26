package com.zero.ecommerce.dto;

import java.time.LocalDate;

/**
 * Fila de "Mis compras" del cliente (E4-04). formaDePago sale de la FacturaCliente y es null si la compra todavía no
 * tiene factura. estado es el nombre del EstadoOrdenCompra, para el badge-estado del kit.
 */
public record CompraClienteDTO(String id, String numero, LocalDate fecha, int cantidadItems, double total,
        String formaDePago, String estado) {
}
