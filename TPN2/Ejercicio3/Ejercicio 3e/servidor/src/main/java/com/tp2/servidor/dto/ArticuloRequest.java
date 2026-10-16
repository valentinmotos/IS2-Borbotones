package com.tp2.servidor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticuloRequest {
    private String nombre;
    private double precio;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }
}
