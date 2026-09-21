package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
public class FacturaProveedor extends Factura {

    public void crearFactura(long numeroFactura, Date fechaFactura, double totalPagado,
                             EstadoFactura estado,
                             List<DetalleFactura> detalles,
                             FormaDePago formaDePago, String idProveedor) {
    }

    public void validar(long numeroFactura, Date fechaFactura, double totalPagado,
                        EstadoFactura estado,
                        List<DetalleFactura> detalles,
                        FormaDePago formaDePago, String idProveedor) {
    }

    public void modificarFactura(String id, long numeroFactura, Date fechaFactura,
                                 double totalPagado, EstadoFactura estado,
                                 List<DetalleFactura> detalles,
                                 String idProveedor) {
    }
}
