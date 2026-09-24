package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.DetalleCompra;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, String> {
}
