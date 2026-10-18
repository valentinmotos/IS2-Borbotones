package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticuloDto {
    private String id;
    private String nombre;
    private double precio;
    private ImagenDto imagen;
    private ProveedorDto proveedor;
}
