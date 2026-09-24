package com.borbotones.integrador1.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DetalleCompra {

    @Id
    private String id;

    private int cantidad;
    private double subtotal;
    private boolean eliminado;

    @ManyToOne
    private Producto producto;

    @ManyToOne
    private OrdenCompra ordenCompra;
}
