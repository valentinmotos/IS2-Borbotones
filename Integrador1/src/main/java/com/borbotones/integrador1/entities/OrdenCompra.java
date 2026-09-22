package com.borbotones.integrador1.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Date;
import java.util.List;

@Entity
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


    public void crearOrdenCompra( String idCliente,  long identificadorCompra,  Date fecha,  double total,  EstadoOrdenCompra estadoOrdenCompra, List<DetalleCompra> detalle ) {
        validar( idCliente, null, identificadorCompra, fecha, total, estadoOrdenCompra, detalle );

        this.id = UUID.randomUUID().toString();
        this.identificadorCompra = identificadorCompra;
        this.fecha = fecha;
        this.total = total;
        this.estadoOrdenCompra = estadoOrdenCompra;
        this.eliminado = false;
        this.detallesCompra = new ArrayList<>();
    }

    public void validar( String idCliente, String idEmpleado, long identificadorCompra,  Date fecha,  double total, EstadoOrdenCompra estadoOrdenCompra,  List<DetalleCompra> detalle ) {
    }

    public void modificarOrdenCompra( String id, String idCliente, long identificadorCompra, Date fecha, double total, EstadoOrdenCompra estadoOrdenCompra, List<DetalleCompra> detalle ) {
    }

    public void eliminarOrdenCompra(String id) {
    }

    public List<OrdenCompra> listarOrdenCompra() {
        return List.of();
    }

    public List<OrdenCompra> listarOrdenCompraActivo() {
        return List.of();
    }

    public List<OrdenCompra> listarOrdenCompraPorEstado( EstadoOrdenCompra estadoOrdenCompra ) {
        return List.of();
    }

    public DetalleCompra crearDetalleCompra( String idProducto, int cantidad ) {
        return null;
    }

    public DetalleCompra buscarDetalleCompra(String id) {
        return null;
    }

    public DetalleCompra modificarDetalleCompra( String idDetalleCompra, String idProducto, int cantidad ) {
        return null;
    }

    public void eliminarDetalleFactura(String idDetalleFactura) {
    }

    public Factura buscarFactura(String id) {
        return null;
    }

}
