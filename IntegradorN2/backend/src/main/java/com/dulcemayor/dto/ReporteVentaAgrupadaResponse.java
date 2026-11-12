package com.dulcemayor.dto;

import java.math.BigDecimal;

public record ReporteVentaAgrupadaResponse(
        String departamentoId,
        String departamento,
        String preventorId,
        String preventor,
        long cantidadVentas,
        BigDecimal montoTotal) {}
