package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.RolUsuario;

/** Datos de la sesión actual (E1-01): GET /api/auth/yo. Nunca incluye la clave. */
public record UsuarioSesionResponse(String id, String nombreUsuario, RolUsuario rol, boolean debeCambiarClave) {
}
