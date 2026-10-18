package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Localidad extends BaseEntity {

    private String nombre;

    private String codigoPostal;

    @ManyToOne
    private Departamento departamento;
}
