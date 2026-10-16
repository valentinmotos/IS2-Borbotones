package com.tp2.servidor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditarDetalleRequest {
    private int cantidad;

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
