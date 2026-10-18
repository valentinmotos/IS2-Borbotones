package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DireccionDto {
    private String calle;
    private Integer numero;
    private String ciudad;
    private Double latitud;
    private Double longitud;
}
