package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.EstadoFactura;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="facturas") @Getter @Setter
public class Factura extends BaseEntity {
    @Column(name="numero_factura", nullable=false, unique=true)
    private long numeroFactura;

    @Column(name="fecha_factura", nullable=false)
    private LocalDate fechaFactura;

    @Column(nullable=false)
    private double total;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30)
    private EstadoFactura estado;

    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="pedido_id", nullable=false, unique=true)
    private Pedido pedido;
}
