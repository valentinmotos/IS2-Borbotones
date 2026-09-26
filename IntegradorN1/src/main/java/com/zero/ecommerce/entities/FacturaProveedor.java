package com.zero.ecommerce.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * Compra de mercadería a un proveedor.
 */
@Entity
@Getter
@Setter
public class FacturaProveedor extends Factura {

    @ManyToOne
    private Proveedor proveedor;

    // Una compra a proveedor ingresa stock.
    @Override
    public int getSignoStock() {
        return 1;
    }

    @Override
    public String describir() {
        return "Compra N.º " + getNumeroFactura();
    }
}
