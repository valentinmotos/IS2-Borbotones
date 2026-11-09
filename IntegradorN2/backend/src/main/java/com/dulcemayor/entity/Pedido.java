package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.EstadoPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
public class Pedido extends BaseEntity {
    @Column(name = "numero_pedido", nullable = false, unique = true)
    private long numeroPedido;

    @Column(name = "fecha_hora_carga", nullable = false)
    private LocalDateTime fechaHoraCarga;

    @Column(nullable = false)
    private double subtotal;

    @Column(nullable = false)
    private double descuento;

    @Column(nullable = false)
    private double total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado;

    @Column(name = "codigo_promocion", length = 50)
    private String codigoPromocion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_de_venta_id", nullable = false)
    private PuntoDeVenta puntoDeVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preventor_id", nullable = false)
    private Preventor preventor;

    /** Composición Pedido 1..n DetallePedido: el detalle se guarda y se borra junto con el pedido. */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    /** Agrega una línea manteniendo las dos puntas de la relación. */
    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }
}
