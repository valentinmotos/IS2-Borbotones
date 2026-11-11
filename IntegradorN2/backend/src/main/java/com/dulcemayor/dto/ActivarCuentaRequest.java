package com.dulcemayor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActivarCuentaRequest(
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.") String correo,
        @NotBlank(message = "El código es obligatorio.")
        @Pattern(regexp = "\\d{6}", message = "El código debe tener seis dígitos.") String codigo) {
}
