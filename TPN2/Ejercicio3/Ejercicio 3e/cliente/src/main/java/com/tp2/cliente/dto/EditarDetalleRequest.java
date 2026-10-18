package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditarDetalleRequest {
    private int cantidad;

    public EditarDetalleRequest() {
    }

    public EditarDetalleRequest(int cantidad) {
        this.cantidad = cantidad;
    }
}
