package com.zero.ecommerce.dto;

public record ActualizacionPrecioDTO(String productoId, String codigo, String nombre, String talle,
        double precioActual, double precioNuevo, double diferencia) {
}