package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.RolUsuario;

/**
 * Respuesta del login y del cambio de clave (contrato "Login y roles" del kickoff, E1-01 → E1-02).
 * El cliente guarda el token y lo manda como "Authorization: Bearer <token>".
 *
 * @param token            JWT firmado
 * @param rol              rol del usuario, para redirigir y armar el menú
 * @param debeCambiarClave true si entró con una clave temporal: el cliente tiene que llevarlo
 *                         a la pantalla de cambio de clave (el servidor bloquea el resto)
 * @param nombreUsuario    correo de la cuenta
 * @param expiraEnSegundos duración del token
 */
public record LoginResponse(String token, RolUsuario rol, boolean debeCambiarClave, String nombreUsuario,
        long expiraEnSegundos) {
}
