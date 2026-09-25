package com.zero.ecommerce.dto;

/** Opción de /api/ubicacion/localidades: incluye el código postal para completarlo en el formulario. */
public record LocalidadDTO(String id, String nombre, String codigoPostal) {
}
