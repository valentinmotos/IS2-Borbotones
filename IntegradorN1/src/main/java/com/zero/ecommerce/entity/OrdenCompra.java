package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.EstadoOrdenCompra;

import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class OrdenCompra {

    @Id
    private String id;

    private String identificadorCompra;

    @Temporal(TemporalType.DATE)
    private java.util.Date fecha;

    private double total;

    @Enumerated(EnumType.STRING)
    private EstadoOrdenCompra estadoOrdenCompra;

    private boolean eliminado;

    @ManyToOne
    private Cliente cliente;

    @OneToMany
    private List<DetalleCompra> detalles;
}
