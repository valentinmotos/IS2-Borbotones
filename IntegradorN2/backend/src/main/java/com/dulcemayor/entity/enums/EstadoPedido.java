package com.dulcemayor.entity.enums;

import com.dulcemayor.exception.ErrorServiceException;
import java.util.Set;

/** Estados del pedido. Cada estado define qué eventos acepta. */
public enum EstadoPedido {
    CARGADO(EventoPedido.MARCAR_SIN_STOCK, EventoPedido.FACTURAR, EventoPedido.ANULAR),
    SIN_STOCK(EventoPedido.FACTURAR, EventoPedido.ANULAR),
    FACTURADO(EventoPedido.INICIAR_REPARTO),
    EN_REPARTO(EventoPedido.ENTREGAR),
    ENTREGADO,
    ANULADO;

    private final Set<EventoPedido> permitidos;

    EstadoPedido(EventoPedido... permitidos) {
        this.permitidos = Set.of(permitidos);
    }

    public EstadoPedido avanzar(EventoPedido evento) {
        if (evento == null || !permitidos.contains(evento)) {
            throw new ErrorServiceException("El pedido está en " + this
                    + " y no admite el evento " + evento + ".", "evento");
        }
        return evento.estadoResultante();
    }
}
