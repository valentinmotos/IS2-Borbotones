package com.zero.ecommerce.dto;

/**
 * Un ítem de la compra en el correo de confirmación (E4-05). Los importes van ya formateados porque el correo se
 * arma en otro hilo, sin la sesión de Hibernate: no se le pasan entidades.
 */
public record ItemCorreoCompraDTO(String producto, int cantidad, String precioUnitario, String subtotal) {
}
