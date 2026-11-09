package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de alta y modificación de una categoría. */
public record CategoriaRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio.")
        @Size(max = 120, message = "El nombre de la categoría no puede superar los 120 caracteres.")
        String nombre) {
}
