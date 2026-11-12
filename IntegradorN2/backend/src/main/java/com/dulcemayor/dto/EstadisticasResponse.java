package com.dulcemayor.dto;

import java.time.LocalDate;
import java.util.List;

public record EstadisticasResponse(List<PedidosPorDia> pedidos, List<StockResponse> stock,
        List<ProductoVendido> productosMasVendidos) {

    public record PedidosPorDia(LocalDate dia, String preventorId, String nombre, String apellido, String legajo,
            long cantidad, long facturados) {
    }

    public record ProductoVendido(String productoId, String codigo, String nombre, long cantidad) {
    }
}
