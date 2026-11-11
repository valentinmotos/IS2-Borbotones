package com.dulcemayor.dto;

/** Datos públicos de un producto disponible para venta mayorista. */
public record ProductoCatalogoDTO(String id, String nombre, String descripcion, String presentacion,
        int unidadesPorBulto, double precioMayorista, String categoriaId, String categoriaNombre, String imagenId) {
}
