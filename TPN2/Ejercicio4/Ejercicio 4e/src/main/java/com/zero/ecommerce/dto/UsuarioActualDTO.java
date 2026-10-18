package com.zero.ecommerce.dto;

import com.zero.ecommerce.entities.enums.RolUsuario;

public record UsuarioActualDTO(String id, String correo, String nombreMostrar, RolUsuario rol) {
}
