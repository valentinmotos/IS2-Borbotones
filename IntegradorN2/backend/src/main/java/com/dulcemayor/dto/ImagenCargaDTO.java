package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoImagen;
import org.springframework.web.multipart.MultipartFile;

public record ImagenCargaDTO(MultipartFile archivo, TipoImagen tipoImagen) {
}
