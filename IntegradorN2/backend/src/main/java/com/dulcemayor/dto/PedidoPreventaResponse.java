package com.dulcemayor.dto;

import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.enums.EstadoPedido;
import java.time.LocalDateTime;

public record PedidoPreventaResponse(String id, long numeroPedido, String comercio, LocalDateTime fechaHoraCarga,
        EstadoPedido estado, double total, String codigoPromocion) {

    public static PedidoPreventaResponse desde(Pedido pedido) {
        return new PedidoPreventaResponse(pedido.getId(), pedido.getNumeroPedido(),
                pedido.getPuntoDeVenta().getNombreComercio(), pedido.getFechaHoraCarga(),
                pedido.getEstado(), pedido.getTotal(), pedido.getCodigoPromocion());
    }
}
