package com.zero.ecommerce.dto;

import java.util.List;

/**
 * Reporte de proveedores (RF31, E5-05). Totales para las tarjetas y una fila por producto con compras recibidas.
 * cantidadProveedores cuenta los proveedores distintos que aparecen en las filas y cantidadConVariosProveedores los
 * productos que se le compraron a más de un proveedor (los que tienen algo para comparar).
 */
public record ReporteProveedoresDTO(int cantidadProductos, int cantidadProveedores, int cantidadConVariosProveedores,
        List<ProductoProveedorDTO> productos) {
}
