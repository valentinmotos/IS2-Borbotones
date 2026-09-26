package com.zero.ecommerce.dto;

/**
 * Un paso de la línea de tiempo del seguimiento de una compra (E4-01). completo indica que el paso ya se cumplió y
 * actual que es el paso en el que está la orden. Lo arma {@code OrdenCompra.pasosSeguimiento()}.
 */
public record PasoSeguimientoDTO(String nombre, boolean completo, boolean actual) {
}
