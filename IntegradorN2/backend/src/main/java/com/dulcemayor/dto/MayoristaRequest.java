package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MayoristaRequest(
        @NotBlank(message = "La razón social es obligatoria.")
        @Size(max = 160, message = "La razón social no puede superar los 160 caracteres.")
        String razonSocial,
        @NotBlank(message = "El CUIT es obligatorio.")
        @Pattern(regexp = "[0-9-]+", message = "Ingresá un CUIT válido.")
        String cuit) {
}
