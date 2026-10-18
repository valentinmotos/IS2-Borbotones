package com.zero.ecommerce.dto;

/**
 * Producto listo para mostrar en la vidriera publica. No expone entidades JPA y
 * concentra los datos calculados que necesitan catalogo, detalle y carrito.
 */
public record ProductoCatalogoDTO(
        String id,
        String codigo,
        String nombre,
        String descripcion,
        String talle,
        String imagenId,
        double precio,
        String precioFormateado,
        boolean enOferta,
        int stock,
        String categoria,
        String subCategoria,
        String subCategoriaId) {

    /** Alias conservado para las vistas y consumidores incorporados en E3-05. */
    public String imagen() {
        return imagenId;
    }

    /** Alias conservado para las vistas y consumidores incorporados en E3-05. */
    public boolean oferta() {
        return enOferta;
    }
}
