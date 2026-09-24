package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DetalleFactura extends BaseEntity {

    private int cantidad;

    private double subtotal;

    @ManyToOne
    private Factura factura;

    @ManyToOne
    private Producto producto;
}
