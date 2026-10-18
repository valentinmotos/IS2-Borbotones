package com.zero.ecommerce.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.DetalleCompra;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, String> {

    Optional<DetalleCompra> findByIdAndEliminadoFalse(String id);
}

