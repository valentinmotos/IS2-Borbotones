package com.dulcemayor.dto;

import org.springframework.web.multipart.MultipartFile;
import com.dulcemayor.entity.enums.TipoImagen;

/** Datos de una imagen enviada como multipart/form-data. */
public record ImagenCargaDTO(MultipartFile archivo, TipoImagen tipoImagen) {
}
