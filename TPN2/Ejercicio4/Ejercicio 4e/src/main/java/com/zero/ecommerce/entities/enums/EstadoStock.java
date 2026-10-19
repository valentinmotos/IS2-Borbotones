package com.zero.ecommerce.entities.enums;

/** Nivel del stock actual respecto del saldo que dejó la última recepción. */
public enum EstadoStock {
    BUENO("Bueno"),
    REGULAR("Regular"),
    MALO("Malo");

    private final String descripcion;

    EstadoStock(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static EstadoStock desdePorcentaje(double porcentaje) {
        if (porcentaje > 50) {
            return BUENO;
        }
        if (porcentaje >= 20) {
            return REGULAR;
        }
        return MALO;
    }
}
