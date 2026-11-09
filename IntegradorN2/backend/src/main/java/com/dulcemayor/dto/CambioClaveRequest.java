package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cambio de clave del usuario logueado (E1-01): PUT /api/auth/clave.
 * Las reglas (largo mínimo, distinta de la actual, confirmación) las aplica UsuarioService.
 */
public record CambioClaveRequest(
        @NotBlank(message = "Ingresá tu clave actual.") String claveActual,
        @NotBlank(message = "Ingresá la clave nueva.") String nuevaClave,
        @NotBlank(message = "Confirmá la clave nueva.") String confirmarClave) {
}
