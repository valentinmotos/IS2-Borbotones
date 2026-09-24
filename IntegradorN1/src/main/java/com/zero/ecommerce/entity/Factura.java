package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.EstadoFactura;

import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class Factura {

    @Id
    private String id;

    private long numeroFactura;

    @Temporal(TemporalType.DATE)
    private java.util.Date fechaFactura;

    private double totalPagado;

    @Enumerated(EnumType.STRING)
    private EstadoFactura estado;

    private boolean eliminado;

    @OneToMany
    private List<DetalleFactura> detalles;

    @ManyToOne
    private FormaDePago formaDePago;
}
