package com.zero.ecommerce.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
}
