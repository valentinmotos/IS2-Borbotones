package com.zero.ecommerce.dto;

import java.util.List;

import com.zero.ecommerce.entities.SubCategoria;

public record CategoriaArbolDTO(String id, String nombre, List<SubCategoria> subCategorias) {
}
