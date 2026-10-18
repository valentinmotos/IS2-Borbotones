package com.tp2.servidor.dto;

import com.tp2.servidor.entity.Direccion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProveedorRequest {
    private String nombre;
    private Direccion direccion;
}
