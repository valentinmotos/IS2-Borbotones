package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.EstadoPedido;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="pedidos")
@Getter
@Setter
public class Pedido extends BaseEntity {
    @Column(name="numero_pedido", nullable=false, unique=true)
    private long numeroPedido;

    @Column(name="fecha_hora_carga", nullable=false)
    private LocalDateTime fechaHoraCarga;

    @Column(nullable=false)
    private double subtotal;

    @Column(nullable=false)
    private double descuento;

    @Column(nullable=false)
    private double total;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30)
    private EstadoPedido estado;

    @Column(name="codigo_promocion", length=50)
    private String codigoPromocion;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="punto_de_venta_id", nullable=false)
    private PuntoDeVenta puntoDeVenta;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="preventor_id", nullable=false)
    private Preventor preventor;
}
