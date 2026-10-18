package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProveedorDto {
    private String id;
    private String nombre;
    private DireccionDto direccion = new DireccionDto();
}
