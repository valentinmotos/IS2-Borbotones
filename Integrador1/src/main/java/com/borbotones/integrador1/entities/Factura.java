package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class Factura {

    @Id
    private String id;

    private long numeroFactura;

    private Date fechaFactura;

    private double totalPagado;

    private EstadoFactura estado;

    private boolean eliminado;

    @OneToMany
    private List<DetalleFactura> detalles;
}
