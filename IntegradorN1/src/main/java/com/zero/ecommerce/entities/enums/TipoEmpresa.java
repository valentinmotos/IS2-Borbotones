package com.zero.ecommerce.entities.enums;

public enum TipoEmpresa {
    SEDE_CENTRAL("Sede central"),
    SUCURSAL("Sucursal");

    private final String descripcion;

    TipoEmpresa(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
