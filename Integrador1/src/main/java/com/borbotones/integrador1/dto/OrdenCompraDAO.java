package com.borbotones.integrador1.dto;

import com.borbotones.integrador1.entities.DetalleCompra;
import com.borbotones.integrador1.entities.OrdenCompra;
import com.borbotones.integrador1.entities.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class OrdenCompraDAO {

    @PersistenceContext
    private EntityManager entityManager;

    public void addDetalleProducto( OrdenCompra ordenCompra, DetalleCompra detalleCompra) {
        ordenCompra.getDetallesCompra().add(detalleCompra);
        entityManager.merge(ordenCompra);
    }

    public void modificaDetalleProducto( OrdenCompra ordenCompra, DetalleCompra detalleCompra, int cantidad) {
        if (cantidad == 0) {
            detalleCompra.setEliminado(true);
        } else if (cantidad > 0) {
            detalleCompra.setCantidad(cantidad);
            double subtotal = detalleCompra.getProducto().getPrecio() * cantidad;
            detalleCompra.setSubtotal(subtotal);
        }
        entityManager.merge(detalleCompra);
    }

    public OrdenCompra buscarOrdenCompra( String id ) {
        return entityManager.find( OrdenCompra.class , id );
    }

}