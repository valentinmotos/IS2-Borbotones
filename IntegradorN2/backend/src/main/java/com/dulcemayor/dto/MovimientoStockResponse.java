package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoMovimientoStock;
import java.time.LocalDateTime;

/** E2-02: un movimiento del historial. {@code cantidad} es con signo (negativa si descuenta). */
public record MovimientoStockResponse(String id, TipoMovimientoStock tipo, int cantidad,
        LocalDateTime fechaHora, String observacion) {
}
