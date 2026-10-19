package com.tp2.servidor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgregarArticuloRequest {
    private String articuloId;
    private int cantidad = 1;
}
