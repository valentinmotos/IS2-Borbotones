package com.dulcemayor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        String departamentoId,
        String preventorId,
        long cantidadVentas,
        BigDecimal montoTotal,
        List<ReporteVentaAgrupadaResponse> resumen,
        List<ReporteVentaDetalleResponse> detalle) {}
