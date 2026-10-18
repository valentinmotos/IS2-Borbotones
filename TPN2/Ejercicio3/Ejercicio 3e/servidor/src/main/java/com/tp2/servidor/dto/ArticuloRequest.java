package com.tp2.servidor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticuloRequest {
    private String nombre;
    private double precio;
    private String imagenId;
    private String proveedorId;
}
