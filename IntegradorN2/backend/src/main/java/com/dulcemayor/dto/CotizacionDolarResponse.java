package com.dulcemayor.dto;

import java.time.LocalDateTime;

public record CotizacionDolarResponse(LocalDateTime fecha, double valorVenta) {
}
