package com.borbotones.integrador1.entities;

import java.util.List;

public class OrdenCompra {
    @Id
    private String id;

    @OneToMany
    private List<DetalleCompra> detallesCompra;

    private String identificadorCompra;

    private Date fecha;

    private double total;

    private EstadoOrdenCompra estadoOrdenCompra;

    private boolean eliminado;


    public void crearOrdenCompra( String idCliente,  long identificadorCompra,  Date fecha,  double total,  EstadoOrdenCompra estadoOrdenCompra, List<DetalleCompra> detalle ) {
    }

    public void validar( String idCliente, String idEmpleado, long identificadorCompra,  Date fecha,  double total, EstadoOrdenCompra estadoOrdenCompra,  List<DetalleCompra> detalle ) {
    }

    public void modificarOrdenCompra( String id, String idCliente, long identificadorCompra, Date fecha, double total, EstadoOrdenCompra estadoOrdenCompra, List<DetalleCompra> detalle ) {
    }

    public void eliminarOrdenCompra(String id) {
    }

    public Collection<OrdenCompra> listarOrdenCompra() {
    }

    public Collection<OrdenCompra> listarOrdenCompraActivo() {
    }

    public Collection<OrdenCompra> listarOrdenCompraPorEstado( EstadoOrdenCompra estadoOrdenCompra ) {
    }

    public DetalleFactura crearDetalleCompra( String idProducto, int cantidad ) {
    }

    public DetalleFactura buscarDetalleCompra(String id) {
    }

    public DetalleFactura modificarDetalleCompra( String idDetalleFactura, String idProducto, int cantidad ) {
    }

    public void eliminarDetalleFactura(String idDetalleFactura) {
    }

    public Factura buscarFactura(String id) {
    }

}
