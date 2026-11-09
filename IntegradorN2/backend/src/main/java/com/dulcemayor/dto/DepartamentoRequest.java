package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartamentoRequest(
        @NotBlank(message = "El nombre del departamento es obligatorio.")
        @Size(max = 120, message = "El nombre del departamento no puede superar los 120 caracteres.")
        String nombre) {
}
