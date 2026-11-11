package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoMovimientoStock;
import java.time.LocalDateTime;

public record MovimientoStockResponse(String id, TipoMovimientoStock tipo, int cantidad,
        LocalDateTime fechaHora, String observacion) {
}
