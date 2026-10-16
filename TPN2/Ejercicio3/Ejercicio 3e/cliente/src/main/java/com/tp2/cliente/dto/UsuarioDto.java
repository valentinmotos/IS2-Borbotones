package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDto {
    private String id;
    private String nombre;
    private String clave;
    private boolean eliminado;

}
