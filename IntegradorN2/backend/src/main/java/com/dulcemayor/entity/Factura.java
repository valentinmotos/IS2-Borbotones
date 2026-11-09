package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.EstadoFactura;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facturas")
@Getter
@Setter
public class Factura extends BaseEntity {
    @Column(name = "numero_factura", nullable = false, unique = true)
    private long numeroFactura;

    @Column(name = "fecha_factura", nullable = false)
    private LocalDate fechaFactura;

    @Column(nullable = false)
    private double total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoFactura estado;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    /** Composición Factura 1..n DetalleFactura: el detalle se guarda y se borra junto con la factura. */
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleFactura> detalles = new ArrayList<>();

    /** Agrega una línea manteniendo las dos puntas de la relación. */
    public void agregarDetalle(DetalleFactura detalle) {
        detalle.setFactura(this);
        detalles.add(detalle);
    }
}
