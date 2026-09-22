package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
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

    public void eliminarFactura(String id) {
    }

    public List<Factura> listarFactura() {
        return List.of();
    }

    public List<Factura> listarFacturaActivo() {
        return List.of();
    }

    public List<Factura> listarFacturaPorEstado(EstadoFactura estado) {
        return List.of();
    }

    public DetalleFactura crearDetalleFactura(String idDetalleCompra) {
        return null;
    }

    public DetalleFactura buscarDetalleFactura(String id) {
        return null;
    }

    public DetalleFactura modificarDetalleFactura( String idDetalleFactura, String idProducto) {
        return null;
    }

    public void eliminarDetalleFactura(String idDetalleFactura) {
    }

    public Factura buscarFactura(String id) {
        return null;
    }
}
