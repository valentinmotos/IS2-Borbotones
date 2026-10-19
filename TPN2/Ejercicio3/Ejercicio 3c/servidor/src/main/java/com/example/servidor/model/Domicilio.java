package com.example.servidor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Domicilio extends EntidadBase {
    private String calle;
    private Integer numero;

    @ManyToOne
    private Localidad localidad;
}
