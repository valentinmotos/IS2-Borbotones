package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.MonedaPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pagos")
@Getter
@Setter
public class Pago extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forma_de_pago_id", nullable = false)
    private FormaDePago formaDePago;

    @Column(nullable = false)
    private double monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MonedaPago moneda;

    @Column(name = "cotizacion_aplicada")
    private Double cotizacionAplicada;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(length = 255)
    private String referencia;

    @Column(length = 500)
    private String observacion;
}
