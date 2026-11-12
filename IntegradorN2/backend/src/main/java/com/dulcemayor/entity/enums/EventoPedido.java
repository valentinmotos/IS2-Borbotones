package com.dulcemayor.entity.enums;

public enum EventoPedido {
    MARCAR_SIN_STOCK("Stock insuficiente para facturar."),
    FACTURAR("Stock verificado y pedido facturado."),
    INICIAR_REPARTO("El pedido salió a reparto."),
    ENTREGAR("El pedido fue entregado."),
    ANULAR("El pedido fue anulado.");

    private final String observacion;

    EventoPedido(String observacion) {
        this.observacion = observacion;
    }

    EstadoPedido estadoResultante() {
        return switch (this) {
            case MARCAR_SIN_STOCK -> EstadoPedido.SIN_STOCK;
            case FACTURAR -> EstadoPedido.FACTURADO;
            case INICIAR_REPARTO -> EstadoPedido.EN_REPARTO;
            case ENTREGAR -> EstadoPedido.ENTREGADO;
            case ANULAR -> EstadoPedido.ANULADO;
        };
    }

    public String observacion() {
        return observacion;
    }
}
