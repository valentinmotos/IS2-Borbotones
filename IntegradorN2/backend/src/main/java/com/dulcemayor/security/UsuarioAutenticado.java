package com.dulcemayor.security;

import com.dulcemayor.entity.enums.RolUsuario;

/**
 * Principal que JwtFiltro deja en el SecurityContext. La identidad de quien llama sale siempre
 * de acá (del token), nunca de un id que mande el cliente:
 * {@code public X miRecurso(@AuthenticationPrincipal UsuarioAutenticado usuario)}.
 *
 * @param claveTemporal true si todavía tiene que cambiar la clave (solo puede usar /api/auth/**)
 */
public record UsuarioAutenticado(String id, String nombreUsuario, RolUsuario rol, boolean claveTemporal) {
}
