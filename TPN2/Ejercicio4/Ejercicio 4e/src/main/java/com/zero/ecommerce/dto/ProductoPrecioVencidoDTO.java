package com.zero.ecommerce.dto;

import java.time.LocalDate;

/** Producto cuyo precio vigente no se actualiza desde hace mas de dos meses. */
public record ProductoPrecioVencidoDTO(
        String productoId,
        String codigo,
        String nombre,
        String talle,
        double precio,
        LocalDate fechaUltimoCambio,
        long diasSinActualizar,
        String categoriaId,
        String categoriaNombre,
        String subCategoriaNombre) {
}
