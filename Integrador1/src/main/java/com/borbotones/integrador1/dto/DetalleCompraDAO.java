package com.borbotones.integrador1.dto;

import com.borbotones.integrador1.entities.DetalleCompra;
import com.borbotones.integrador1.entities.OrdenCompra;
import com.borbotones.integrador1.entities.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class DetalleCompraDAO {

    @PersistenceContext
    private EntityManager entityManager;

    public DetalleCompra crearDetalleCompra(Producto producto) {

        DetalleCompra detalleCompra = new DetalleCompra();

        detalleCompra.setId(UUID.randomUUID().toString());
        detalleCompra.setProducto(producto);
        detalleCompra.setCantidad(0);
        detalleCompra.setSubtotal(0.0);
        detalleCompra.setEliminado(false);

        entityManager.persist(detalleCompra);

        return detalleCompra;
    }

    public void eliminarDetalleCompra(DetalleCompra detalleCompra) {
        detalleCompra.setEliminado(true);
        entityManager.merge(detalleCompra);
    }

    public void modificarCantidadDetalleCompra( DetalleCompra detalleCompra, int cantidad) {

        if (cantidad >= 0) {
            detalleCompra.setCantidad(cantidad);
            entityManager.merge(detalleCompra);
        }
    }

    public DetalleCompra buscarDetalleCompra(String id ) {
        return entityManager.find( DetalleCompra.class , id );
    }

}