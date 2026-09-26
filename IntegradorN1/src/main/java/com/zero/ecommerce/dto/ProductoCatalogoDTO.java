package com.zero.ecommerce.dto;

/**
 * Producto listo para mostrar en la vidriera publica. No expone entidades JPA a
 * las vistas y concentra los datos que necesitan el catalogo y el carrito.
 */
public record ProductoCatalogoDTO(
        String id,
        String codigo,
        String nombre,
        String talle,
        String imagen,
        double precio,
        boolean oferta,
        int stock) {
}
