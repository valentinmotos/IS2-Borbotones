package com.zero.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Provincia {

    @Id
    private String id;

    private String nombre;
    private boolean eliminado;

    @ManyToOne
    private Pais pais;
}