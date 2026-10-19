package com.zero.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoriaForm {

    @NotBlank(message = "El nombre de la categoría es obligatorio.")
    private String nombre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
