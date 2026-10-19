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

    /** Experto: precio unitario del renglón, derivado del subtotal (el diagrama no lo guarda aparte). */
    public double getPrecioUnitario() {
        return cantidad == 0 ? 0 : subtotal / cantidad;
    }
}
