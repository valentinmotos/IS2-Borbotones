package com.zero.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Stock {

    @Id
    private String id;

    private int cantidadActual;
    private String observacion;
    private boolean eliminado;

    @OneToOne
    private Producto producto;
}
