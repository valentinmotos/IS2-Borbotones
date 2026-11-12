package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Intento persistido de Checkout Pro vía Orders; permite asociar y deduplicar webhooks. */
@Entity
@Table(name = "ordenes_pago_mercado_pago", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mp_orden_idempotencia", columnNames = "clave_idempotencia"),
        @UniqueConstraint(name = "uk_mp_orden_externa", columnNames = "orden_externa_id")
})
@Getter
@Setter
public class OrdenPagoMercadoPago extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(name = "clave_idempotencia", nullable = false, length = 36)
    private String claveIdempotencia;

    @Column(name = "orden_externa_id", length = 80)
    private String ordenExternaId;

    @Column(name = "referencia_externa", nullable = false, length = 36)
    private String referenciaExterna;

    @Column(name = "url_checkout", length = 1000)
    private String urlCheckout;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Column(name = "detalle_estado", length = 100)
    private String detalleEstado;

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_pagado", precision = 19, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "pago_externo_id", length = 80)
    private String pagoExternoId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
