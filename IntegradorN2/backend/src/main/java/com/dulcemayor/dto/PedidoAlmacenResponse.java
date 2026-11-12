package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.EstadoPedido;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoAlmacenResponse(Resumen pedido, double subtotal, double descuento,
        List<Linea> detalles, List<FaltanteStockDTO> faltantes, FacturaResponse factura,
        List<PagoResponse> pagos) {

    public record Resumen(String id, long numeroPedido, String comercio, LocalDateTime fechaHoraCarga,
            EstadoPedido estado, double total) {}

    public record Linea(String id, String producto, String codigo, int cantidad, double precioUnitario,
            double subtotal) {}
}
