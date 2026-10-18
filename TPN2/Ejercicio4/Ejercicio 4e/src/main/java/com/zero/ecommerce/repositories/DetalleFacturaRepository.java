package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.DetalleFactura;

public interface DetalleFacturaRepository extends JpaRepository<DetalleFactura, String> {
}
