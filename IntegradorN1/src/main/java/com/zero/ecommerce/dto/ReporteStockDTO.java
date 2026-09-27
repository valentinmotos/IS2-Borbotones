package com.zero.ecommerce.dto;

import java.util.List;

/** Resultado completo de RF29 para la sede central. */
public record ReporteStockDTO(
        String sucursalId,
        String sucursal,
        int totalUnidades,
        int cantidadBueno,
        int cantidadRegular,
        int cantidadMalo,
        List<ProductoStockDTO> productos) {

    public int cantidadProductos() {
        return productos.size();
    }
}
