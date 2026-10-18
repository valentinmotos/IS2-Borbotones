package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CarritoDto {
    private String id;
    private double total;
    private boolean eliminado;
    private boolean finalizado;
    private UsuarioDto usuario;
    private List<DetalleDto> detalles = new ArrayList<>();

}
