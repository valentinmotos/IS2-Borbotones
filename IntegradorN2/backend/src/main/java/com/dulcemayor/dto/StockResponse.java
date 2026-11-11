package com.dulcemayor.dto;

public record StockResponse(String productoId, String codigo, String nombre, String presentacion,
        int cantidadActual, boolean sinStock) {
}
