package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * Venta a un cliente, creada desde su OrdenCompra al confirmar el carrito.
 */
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

    // Una venta descuenta stock.
    @Override
    public int getSignoStock() {
        return -1;
    }

    @Override
    public String describir() {
        return "Venta N.º " + getNumeroFactura();
    }
}
