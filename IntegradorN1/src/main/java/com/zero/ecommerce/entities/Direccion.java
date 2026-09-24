package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Direccion extends BaseEntity {

    private String calle;

    private String numeracion;

    private String barrio;

    private String manzanaPiso;

    private String casaDepartamento;

    private String referencia;

    @ManyToOne
    private Localidad localidad;
}
