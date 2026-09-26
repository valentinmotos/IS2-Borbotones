package com.zero.ecommerce.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

/**
 * Carrito del cliente. La venta es la FacturaCliente que se crea al confirmarlo.
 */
@Entity
@Getter
@Setter
public class OrdenCompra extends BaseEntity {

    private String identificadorCompra;

    private LocalDate fecha;

    private double total;

    @Enumerated(EnumType.STRING)
    private EstadoOrdenCompra estadoOrdenCompra;

    @ManyToOne
    private Cliente cliente;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCompra> detalles = new ArrayList<>();

    /**
     * Creador: la orden crea sus detalles. El subtotal es cantidad * precio unitario; el precio unitario no se
     * guarda aparte porque el diagrama no lo tiene (se recalcula con {@link DetalleCompra#getPrecioUnitario()}).
     */
    public DetalleCompra crearDetalle(Producto producto, int cantidad, double precioUnitario) {
        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.calcularSubtotal(precioUnitario);
        detalle.setOrdenCompra(this);
        this.detalles.add(detalle);
        recalcularTotal();
        return detalle;
    }

    /** Experto: la orden sabe calcular su total como la suma de los subtotales de sus detalles activos. */
    public double recalcularTotal() {
        this.total = this.detalles.stream()
                .filter(d -> !d.isEliminado())
                .mapToDouble(DetalleCompra::getSubtotal)
                .sum();
        return this.total;
    }

    /** Busca si ya existe un ítem en el carrito para el producto dado. */
    public Optional<DetalleCompra> buscarDetallePorProducto(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return Optional.empty();
        }
        return this.detalles.stream()
                .filter(d -> !d.isEliminado() && d.getProducto() != null && idProducto.equals(d.getProducto().getId()))
                .findFirst();
    }

    /** Cantidad total de unidades de productos en el carrito. */
    public int getCantidadTotalItems() {
        return this.detalles.stream()
                .filter(d -> !d.isEliminado())
                .mapToInt(DetalleCompra::getCantidad)
                .sum();
    }
}

