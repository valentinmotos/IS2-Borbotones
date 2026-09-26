package com.zero.ecommerce.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.zero.ecommerce.entities.enums.EstadoFactura;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Factura extends BaseEntity {

    private long numeroFactura;

    private LocalDate fechaFactura;

    private double totalPagado;

    @Enumerated(EnumType.STRING)
    private EstadoFactura estado;

    @ManyToOne
    private FormaDePago formaDePago;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleFactura> detalles = new ArrayList<>();

    /**
     * Creador: la factura crea sus detalles. El subtotal es cantidad × precio unitario; el precio unitario no se
     * guarda aparte porque el diagrama no lo tiene (se recalcula con {@link DetalleFactura#getPrecioUnitario()}).
     */
    public DetalleFactura agregarDetalle(Producto producto, int cantidad, double precioUnitario) {
        DetalleFactura detalle = new DetalleFactura();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setSubtotal(cantidad * precioUnitario);
        detalle.setFactura(this);
        detalles.add(detalle);
        return detalle;
    }

    /** Experto: la factura sabe calcular su total como la suma de los subtotales de sus detalles activos. */
    public double calcularTotal() {
        return detalles.stream()
                .filter(d -> !d.isEliminado())
                .mapToDouble(DetalleFactura::getSubtotal)
                .sum();
    }

    /**
     * Signo con el que los detalles de esta factura mueven el stock: +1 entra, -1 sale.
     */
    public abstract int getSignoStock();

    /** Polimorfismo: cómo se nombra la factura en los movimientos de stock ("Compra N.º 3", "Venta N.º 1"). */
    public abstract String describir();
}
