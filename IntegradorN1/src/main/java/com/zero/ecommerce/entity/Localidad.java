package com.zero.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Localidad {

    @Id
    private String id;

    private String nombre;
    private String codigoPostal;
    private boolean eliminado;

    @ManyToOne
    private Departamento departamento;
}