package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReporteVentaDetalleResponse(
        String facturaId,
        long numeroFactura,
        LocalDate fechaFactura,
        String pedidoId,
        long numeroPedido,
        LocalDateTime fechaPedido,
        String puntoDeVenta,
        String cliente,
        String departamentoId,
        String departamento,
        String preventorId,
        String preventor,
        EstadoPedido estadoEnvio,
        BigDecimal monto) {}
