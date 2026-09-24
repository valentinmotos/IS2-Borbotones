package com.zero.ecommerce.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * Movimiento de stock generado por un DetalleFactura. cantidadActual es el saldo del producto
 * después del movimiento; el stock actual es el cantidadActual del último movimiento.
 */
@Entity
@Getter
@Setter
public class Stock extends BaseEntity {

    private int cantidadActual;

    private String observacion;

    // Fecha del movimiento, para ordenar y encontrar el último.
    private LocalDateTime fecha;

    @ManyToOne
    private DetalleFactura detalleFactura;

    @ManyToOne
    private Producto producto;
}
