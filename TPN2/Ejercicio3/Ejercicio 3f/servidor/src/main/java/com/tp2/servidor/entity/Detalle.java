package com.tp2.servidor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Detalle {
    @Id
    private String id = UUID.randomUUID().toString();
    private int cantidad;
    private double precioUnitario;
    private boolean eliminado;

    @ManyToOne
    @JsonIgnore
    private Carrito carrito;

    @ManyToOne
    private Articulo articulo;
}
