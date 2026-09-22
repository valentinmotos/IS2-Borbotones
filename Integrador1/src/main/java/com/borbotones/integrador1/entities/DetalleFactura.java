package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class DetalleFactura {

    @Id
    private String id;

    @OneToMany
    private List<Producto> detalles;

    private int cantidad;

    private double subtotal;

    private boolean eliminado;
}
