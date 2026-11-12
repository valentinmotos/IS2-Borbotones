package com.dulcemayor.dto;

/** E4-04: producto del pedido cuyo stock no alcanza para facturar. */
public record FaltanteStockDTO(String productoId, String producto, int cantidadPedida, int stockDisponible) {
}
