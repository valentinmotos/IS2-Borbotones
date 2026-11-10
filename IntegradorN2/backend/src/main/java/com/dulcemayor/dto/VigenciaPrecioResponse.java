package com.dulcemayor.dto;

import java.time.LocalDate;

/** E2-03: una vigencia del historial de precios. vigente = no tiene fecha de cierre. */
public record VigenciaPrecioResponse(String id, String productoId, LocalDate fechaDesde, LocalDate fechaHasta,
        double precio, boolean vigente) {
}
