package com.zero.ecommerce.dto;

import com.zero.ecommerce.entities.enums.EstadoStock;

/** Una fila del reporte de productos y stock. */
public record ProductoStockDTO(
        String productoId,
        String codigo,
        String nombre,
        String talle,
        String categoriaId,
        String categoriaNombre,
        String subCategoriaNombre,
        int stockActual,
        int stockReferencia,
        int porcentaje,
        int porcentajeBarra,
        EstadoStock estado) {
}
