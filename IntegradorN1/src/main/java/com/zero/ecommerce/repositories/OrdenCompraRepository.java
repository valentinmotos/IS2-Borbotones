package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, String> {

    Optional<OrdenCompra> findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
            String clienteId, EstadoOrdenCompra estado);

    List<OrdenCompra> findByCliente_IdAndEliminadoFalseOrderByFechaDesc(String clienteId);

    List<OrdenCompra> findByEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(EstadoOrdenCompra estado);

    List<OrdenCompra> findByEliminadoFalseOrderByFechaDesc();
}

