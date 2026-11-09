package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoImagen;

public record ImagenDTO(String id, String nombre, String mime, TipoImagen tipoImagen, String url) {
}
