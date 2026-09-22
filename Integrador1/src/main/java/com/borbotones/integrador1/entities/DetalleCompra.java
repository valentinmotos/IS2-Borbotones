package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class DetalleCompra {

    @Id
    private String id;

    @OneToMany
    private List<Producto> detalles;

    private int cantidad;

    private Double subtotal;

    private boolean eliminado;

    // Constructores
    public DetalleCompra() {
    }

}
