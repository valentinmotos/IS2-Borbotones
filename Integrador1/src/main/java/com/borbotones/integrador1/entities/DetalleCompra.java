package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import java.util.List;

@Entity
@Getter
@Setter
public class DetalleCompra {

    @Id
    private String id;

    @ManyToOne
    private Producto producto;

    private int cantidad;

    private Double subtotal;

    private boolean eliminado;

    public DetalleCompra() {
    }

}
