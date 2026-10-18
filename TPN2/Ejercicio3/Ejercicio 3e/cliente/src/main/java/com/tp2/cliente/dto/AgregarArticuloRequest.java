package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgregarArticuloRequest {
    private String articuloId;
    private int cantidad;

    public AgregarArticuloRequest() {
    }

    public AgregarArticuloRequest(String articuloId, int cantidad) {
        this.articuloId = articuloId;
        this.cantidad = cantidad;
    }
}
