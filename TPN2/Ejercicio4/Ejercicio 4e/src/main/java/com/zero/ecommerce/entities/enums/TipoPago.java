package com.zero.ecommerce.entities.enums;

public enum TipoPago {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    BILLETERA_VIRTUAL("Billetera virtual");

    private final String descripcion;

    TipoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
