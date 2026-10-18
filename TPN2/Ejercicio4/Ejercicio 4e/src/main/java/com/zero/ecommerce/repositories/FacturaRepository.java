package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Factura;

public interface FacturaRepository extends JpaRepository<Factura, String> {
}
