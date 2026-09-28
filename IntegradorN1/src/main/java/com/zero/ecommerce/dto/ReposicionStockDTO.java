package com.zero.ecommerce.dto;

/** Datos calculados para reponer un producto cuyo nivel de stock es Malo (RF30). */
public record ReposicionStockDTO(
        String productoId,
        String codigo,
        String nombre,
        String talle,
        int unidades,
        String proveedorId,
        String proveedorNombre) {

    public boolean tieneProveedorRecomendado() {
        return proveedorId != null;
    }
}
