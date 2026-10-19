package com.tp2.servidor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImagenRequest {
    private String nombre;
    private String mime;
    private byte[] contenido;
}
