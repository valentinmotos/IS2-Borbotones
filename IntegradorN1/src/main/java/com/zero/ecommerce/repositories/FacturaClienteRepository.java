package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.FacturaCliente;

public interface FacturaClienteRepository extends JpaRepository<FacturaCliente, String> {

    /** La factura de un pedido: la relación está solo del lado de la factura (FacturaCliente → OrdenCompra). */
    Optional<FacturaCliente> findFirstByOrdenCompra_IdAndEliminadoFalse(String ordenCompraId);

    /** Las facturas asociadas a un pedido, para armar el listado de pedidos sin una consulta por fila. */
    List<FacturaCliente> findByOrdenCompraIsNotNullAndEliminadoFalse();
}
