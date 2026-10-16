package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetalleDto {
    private String id;
    private int cantidad;
    private double precioUnitario;
    private boolean eliminado;
    private ArticuloDto articulo;

}
