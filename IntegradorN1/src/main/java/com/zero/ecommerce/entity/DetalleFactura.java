package com.zero.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DetalleFactura {

    @Id
    private String id;

    private int cantidad;
    private double subtotal;
    private boolean eliminado;

    @ManyToOne
    private Producto producto;

    @ManyToOne
    private Factura factura;
}