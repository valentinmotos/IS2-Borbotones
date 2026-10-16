package com.tp2.servidor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearCarritoRequest {
    private String usuarioId;

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }
}
