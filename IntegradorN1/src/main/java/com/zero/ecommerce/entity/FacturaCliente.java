package com.zero.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class FacturaCliente extends Factura {

    @ManyToOne
    private Cliente cliente;

    @ManyToOne
    private Empleado empleado;

    @OneToOne
    private OrdenCompra ordenCompra;
}