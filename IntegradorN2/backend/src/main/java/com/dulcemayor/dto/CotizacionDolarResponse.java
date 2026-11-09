package com.dulcemayor.dto;

import java.time.LocalDate;

/** Cotización del dólar oficial (vendedor) de un día (E1-08). */
public record CotizacionDolarResponse(LocalDate fecha, double valorVenta) {
}
