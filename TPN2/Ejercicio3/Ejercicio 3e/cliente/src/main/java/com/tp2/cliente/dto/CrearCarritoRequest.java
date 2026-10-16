package com.tp2.cliente.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearCarritoRequest {
    private String usuarioId;

    public CrearCarritoRequest() {
    }

    public CrearCarritoRequest(String usuarioId) {
        this.usuarioId = usuarioId;
    }
}
