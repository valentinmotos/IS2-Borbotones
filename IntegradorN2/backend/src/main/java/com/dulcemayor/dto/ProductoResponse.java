package com.dulcemayor.dto;

public record ProductoResponse(String id, String codigo, String nombre, String descripcion, String presentacion,
        int unidadesPorBulto, String categoriaId, String categoriaNombre, String imagenId, boolean enOferta) {
}
