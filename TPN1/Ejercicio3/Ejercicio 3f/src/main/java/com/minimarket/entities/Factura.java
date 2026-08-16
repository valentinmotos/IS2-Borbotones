package com.minimarket.entities;

import com.minimarket.enums.EstadoFactura;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private long numeroFactura;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date fechaFactura;

    @Column(nullable = false)
    private double totalPagado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFactura estado;

    @Column(nullable = false)
    private boolean eliminado = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    public Factura() {
    }

    public Factura(long numeroFactura, Date fechaFactura, double totalPagado, EstadoFactura estado, Cliente cliente) {
        this.numeroFactura = numeroFactura;
        this.fechaFactura = fechaFactura;
        this.totalPagado = totalPagado;
        this.estado = estado;
        this.cliente = cliente;
        this.eliminado = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(long numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public Date getFechaFactura() {
        return fechaFactura;
    }

    public void setFechaFactura(Date fechaFactura) {
        this.fechaFactura = fechaFactura;
    }

    public double getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(double totalPagado) {
        this.totalPagado = totalPagado;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public void setEstado(EstadoFactura estado) {
        this.estado = estado;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    @Override
    public String toString() {
        return "ID: " + id +
                " | Nro: " + numeroFactura +
                " | Fecha: " + fechaFactura +
                " | Total: $" + totalPagado +
                " | Estado: " + estado +
                " | Cliente: " + (cliente != null ? cliente.getNombre() + " " + cliente.getApellido() : "N/A") +
                " | Eliminado: " + eliminado;
    }
}
