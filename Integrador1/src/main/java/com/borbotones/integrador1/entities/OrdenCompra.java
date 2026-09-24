package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class OrdenCompra {
    @Id
    private String id;

    @OneToMany
    private List<DetalleCompra> detallesCompra;

    @OneToOne
    private Cliente cliente;

    private String identificadorCompra;

    private Date fecha;

    private double total;

    private EstadoOrdenCompra estadoOrdenCompra;

    private boolean eliminado;
}
