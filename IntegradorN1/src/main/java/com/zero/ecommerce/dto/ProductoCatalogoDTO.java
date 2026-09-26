package com.zero.ecommerce.dto;

/**
 * Proyeccion de solo lectura para la vidriera publica. Mantiene fuera de la vista
 * las entidades y concentra los datos calculados de precio y stock.
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
}
