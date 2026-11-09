package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cotizaciones_dolar")
@Getter
@Setter
public class CotizacionDolar extends BaseEntity {
    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(name = "valor_venta", nullable = false)
    private double valorVenta;
}
