package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Ingreso con correo y clave (E1-01): POST /api/auth/login.
 *
 * @param nombreUsuario correo electrónico de la cuenta
 * @param clave         clave en texto plano (solo viaja en la petición; se compara contra el hash BCrypt)
 */
public record LoginRequest(
        @NotBlank(message = "Ingresá tu correo electrónico.") String nombreUsuario,
        @NotBlank(message = "Ingresá tu clave.") String clave) {
}
