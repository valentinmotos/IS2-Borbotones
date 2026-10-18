package com.zero.ecommerce.entities.enums;

public enum Sexo {
    FEMENINO("Femenino"),
    MASCULINO("Masculino"),
    OTRO("Otro");

    private final String descripcion;

    Sexo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
