package com.dulcemayor.dto;

/** E2-02: saldo actual de un producto. {@code sinStock} marca la alerta de stock cero. */
public record StockResponse(String productoId, String codigo, String nombre, String presentacion,
        int cantidadActual, boolean sinStock) {

    public StockResponse(String productoId, String codigo, String nombre, String presentacion,
            Integer cantidadActual) {
        this(productoId, codigo, nombre, presentacion, cantidadActual == null ? 0 : cantidadActual,
                cantidadActual == null || cantidadActual == 0);
    }
}
