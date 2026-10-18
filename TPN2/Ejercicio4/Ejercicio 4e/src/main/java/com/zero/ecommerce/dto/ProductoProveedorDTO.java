package com.zero.ecommerce.dto;

import java.util.List;

/**
 * Una fila del reporte de proveedores (E5-05): el producto, el proveedor recomendado (el de menor precio de costo) y
 * la comparación con el último precio de cada proveedor, del más barato al más caro. recomendado es el primero de
 * proveedores.
 */
public record ProductoProveedorDTO(String idProducto, String codigo, String nombre, String talle, String categoria,
        String subCategoria, PrecioProveedorDTO recomendado, List<PrecioProveedorDTO> proveedores) {
}
