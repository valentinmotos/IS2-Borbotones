package com.zero.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class SubCategoriaForm {

    @NotBlank(message = "El nombre de la subcategoría es obligatorio.")
    private String nombre;

    private String categoriaId;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }
}
