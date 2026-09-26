package com.zero.ecommerce.dto;

/** Fila del ABM: no expone la clave ni la entidad JPA a la vista. */
public record UsuarioAdminDTO(String id, String nombre, String correo, String rol, String estado,
        boolean empleado, boolean cliente) {
}
