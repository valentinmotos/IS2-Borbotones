package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;

import java.util.Date;
import java.util.Collection;

@Entity
public class FacturaCliente extends Factura {

    public void crearFactura(String idCliente, String idEmpleado, long numeroFactura, Date fechaFactura, double totalPagado, EstadoFactura estado,
                             Collection<DetalleFactura> detalles,
                             FormaDePago formaDePago, String idOrdenCompra) {
    }

    public void validar(String idCliente, String idEmpleado, long numeroFactura,
                        Date fechaFactura, double totalPagado, EstadoFactura estado,
                        Collection<DetalleFactura> detalles,
                        FormaDePago formaDePago, String idOrdenCompra) {
    }

    public void modificarFactura(String id, String idCliente, String idEmpleado, long numeroFactura, Date fechaFactura, double totalPagado,
                                 EstadoFactura estado,
                                 Collection<DetalleFactura> detalles,
                                 String idOrdenCompra) {
    }
}
