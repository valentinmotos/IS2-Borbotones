package com.zero.ecommerce.entities.enums;

public enum TipoTelefono {
    FIJO("Fijo"),
    CELULAR("Celular");

    private final String descripcion;

    TipoTelefono(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
