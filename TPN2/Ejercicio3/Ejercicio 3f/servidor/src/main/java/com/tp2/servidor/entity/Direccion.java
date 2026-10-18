package com.tp2.servidor.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Direccion {
    private String calle;
    private Integer numero;
    private String ciudad;
    private Double latitud;
    private Double longitud;
}
