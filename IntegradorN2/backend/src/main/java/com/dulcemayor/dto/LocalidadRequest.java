package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocalidadRequest(
        @NotBlank(message = "El nombre de la localidad es obligatorio.")
        @Size(max = 120, message = "El nombre de la localidad no puede superar los 120 caracteres.")
        String nombre,
        @NotBlank(message = "El código postal es obligatorio.")
        @Pattern(regexp = "\\d{4}", message = "El código postal debe tener 4 dígitos.")
        String codigoPostal,
        @NotBlank(message = "Debés seleccionar un departamento.")
        String departamentoId) {
}
