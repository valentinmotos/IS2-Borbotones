package com.dulcemayor.dto;

import java.time.LocalDate;

public record PromocionResponse(String id, String codigo, String descripcion, double porcentajeDescuento,
        Integer cantidadMinima, String idProducto, String nombreProducto, LocalDate fechaDesde, LocalDate fechaHasta) {
}
